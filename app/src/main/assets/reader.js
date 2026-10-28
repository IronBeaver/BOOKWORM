/* BOOKWORM trusted ES5 engine. No publisher scripts or native JavaScript bridge. */
(function () {
    'use strict';
    var nodes = [], starts = [], total = 0, paged = false, margin = 20, width = 1, height = 1, page = 0, pages = 1;
    function index() {
        nodes = []; starts = []; total = 0;
        var walker = document.createTreeWalker(document.getElementById('bw-content'), NodeFilter.SHOW_TEXT, null, false), n;
        while ((n = walker.nextNode())) { starts.push(total); nodes.push(n); total += n.length; }
    }
    function rangeAt(offset) {
        if (!nodes.length) return null;
        offset = Math.max(0, Math.min(total - 1, offset));
        var i = 0;
        while (i + 1 < starts.length && starts[i + 1] <= offset) i++;
        var r = document.createRange(), pos = Math.min(nodes[i].length, offset - starts[i]);
        r.setStart(nodes[i], pos); r.setEnd(nodes[i], Math.min(nodes[i].length, pos + 1)); return r;
    }
    function anchor() {
        var r, i, x, y;
        // A caret in the top visible text line gives a stable character anchor, not a pixel offset.
        if (document.caretRangeFromPoint) {
            for (y = margin + 2; y < Math.min(height, margin + 120); y += 12) {
                for (x = margin + 2; x < Math.max(margin + 3, width - margin); x += 24) {
                    r = document.caretRangeFromPoint(x, y);
                    if (r && r.startContainer.nodeType === 3 && /\S/.test(r.startContainer.nodeValue)) {
                        i = nodes.indexOf(r.startContainer);
                        if (i >= 0) return starts[i] + r.startOffset;
                    }
                }
            }
        }
        for (i = 0; i < nodes.length; i++) {
            if (!/\S/.test(nodes[i].nodeValue)) continue;
            r = document.createRange(); r.selectNodeContents(nodes[i]);
            var rects = r.getClientRects(), j;
            for (j = 0; j < rects.length; j++) if (rects[j].right > 0 && rects[j].left < width && rects[j].bottom > 0 && rects[j].top < height) return starts[i];
        }
        return total;
    }
    function position() {
        var offset = anchor();
        return {offset: offset, fraction: total ? offset / total : 0, page: page + 1, pages: pages};
    }
    function go(offset, fragment) {
        var element = fragment ? document.getElementById('pub-' + fragment) : null;
        var r = element ? element.getBoundingClientRect() : null;
        if (!r) { var range = rangeAt(offset < 0 ? total - 1 : offset); r = range ? range.getBoundingClientRect() : {left: 0, top: 0}; }
        if (paged) {
            page = Math.max(0, Math.min(pages - 1, offset < 0 && !fragment ? pages - 1 : Math.floor((r.left + window.pageXOffset) / width)));
            window.scrollTo(page * width, 0);
        } else window.scrollTo(0, offset < 0 && !fragment ? document.body.scrollHeight : Math.max(0, r.top + window.pageYOffset - margin));
        return position();
    }
    window.BW = {
        init: function (isPaged, inset, offset, fragment) {
            paged = isPaged; margin = inset; width = Math.max(1, window.innerWidth); height = Math.max(1, window.innerHeight);
            var main = document.getElementById('bw-content');
            if (paged) {
                document.documentElement.style.overflow = 'hidden'; document.body.style.overflow = 'visible';
                document.body.style.width = width + 'px'; document.body.style.height = height + 'px';
                main.style.padding = '0'; main.style.margin = margin + 'px';
                main.style.width = Math.max(1, width - 2 * margin) + 'px'; main.style.height = Math.max(1, height - 2 * margin) + 'px';
                main.style.webkitColumnWidth = Math.max(1, width - 2 * margin) + 'px';
                main.style.webkitColumnGap = (2 * margin) + 'px'; main.style.webkitColumnFill = 'auto';
                main.style.columnWidth = main.style.webkitColumnWidth; main.style.columnGap = main.style.webkitColumnGap; main.style.columnFill = 'auto';
                pages = Math.max(1, Math.ceil((main.scrollWidth + 2 * margin) / width));
            }
            index();
            var touchOptions = false;
            try {
                var probe = Object.defineProperty({}, 'passive', { get: function () { touchOptions = {passive: false}; return false; } });
                var noop = function () {};
                window.addEventListener('bw-passive-probe', noop, probe);
                window.removeEventListener('bw-passive-probe', noop, probe);
            } catch (unsupported) { /* Older WebViews use the boolean capture argument. */ }
            document.addEventListener('touchmove', function (event) {
                if (!paged || !event.cancelable) return;
                var selected = window.getSelection ? window.getSelection() : null;
                if (selected && selected.rangeCount && !selected.isCollapsed) return;
                event.preventDefault();
            }, touchOptions);
            return go(offset, fragment);
        },
        position: position,
        turn: function (direction) {
            if (!paged) { window.scrollBy(0, direction * height * 0.85); return {boundary: 0}; }
            if (page + direction < 0 || page + direction >= pages) return {boundary: direction};
            page += direction; window.scrollTo(page * width, 0); return {boundary: 0};
        }
    };
}());
