(function () {
    'use strict';
    // ES5 + DOM measurements work without promises, FontFace or document.fonts.
    var sample = 'mmmmmmmmmmWWWWWWWWWWiiiiiiiiii0123456789 affinity office';
    var fallbacks = ['monospace', 'serif', 'sans-serif'];
    var gate = document.getElementById('gate'), negative = document.getElementById('negative');
    var cases = [], sections = document.querySelectorAll('section[data-face]');
    var start = Date.now(), stable = 0, previousGeometry = '', stopped = false;
    var frame = window.requestAnimationFrame || function (callback) { return window.setTimeout(callback, 16); };

    function span(family, weight, style) {
        var node = document.createElement('span');
        node.className = 'probe'; node.setAttribute('aria-hidden', 'true');
        node.style.fontFamily = family; node.style.fontWeight = weight; node.style.fontStyle = style;
        node.textContent = sample; document.body.appendChild(node); return node;
    }
    function probe(family, weight, style) {
        var test = [], base = [], i;
        for (i = 0; i < fallbacks.length; i++) {
            base.push(span(fallbacks[i], weight, style));
            test.push(span(family + ',' + fallbacks[i], weight, style));
        }
        return {test: test, base: base, ready: false, width: 0};
    }
    function detected(p) {
        var width = p.test[0].getBoundingClientRect().width, different = false, i;
        if (width <= 0) return false;
        for (i = 0; i < fallbacks.length; i++) {
            if (Math.abs(p.test[i].getBoundingClientRect().width - width) > 0.1) return false;
            if (Math.abs(p.base[i].getBoundingClientRect().width - width) > 0.5) different = true;
        }
        p.width = width;
        return different;
    }
    for (var i = 0; i < sections.length; i++) {
        var section = sections[i];
        var item = probe(section.getAttribute('data-face'), section.getAttribute('data-weight'), section.getAttribute('data-style'));
        item.label = section.querySelector('.face-state'); cases.push(item);
    }
    var missing = probe('BWDeliberatelyMissing', '400', 'normal');

    function cleanup() {
        var nodes = document.querySelectorAll('.probe'), i;
        for (i = 0; i < nodes.length; i++) nodes[i].parentNode.removeChild(nodes[i]);
    }
    function tick() {
        if (stopped) return;
        var count = 0, negativeFailed = detected(missing), widths = [], i;
        for (i = 0; i < cases.length; i++) {
            var c = cases[i];
            c.ready = detected(c); widths.push(c.width);
            if (c.ready) {
                count++;
                c.label.textContent = 'Detected in WebView — probe width ' + c.width.toFixed(2) + ' px';
                c.label.className = 'face-state ready';
            }
        }
        negative.textContent = negativeFailed ? 'FAIL: missing font was falsely detected.' : 'Negative control: missing font is NOT detected (expected).';
        negative.className = negativeFailed ? 'failed' : 'ready';
        // Detect all faces first; stable geometry is an additional barrier, never a timer substitute.
        var geometry = widths.join(',') + ':' + document.documentElement.clientWidth + ':' + document.body.scrollHeight;
        stable = count === cases.length && cases.length === 16 && !negativeFailed && geometry === previousGeometry ? stable + 1 : 0;
        previousGeometry = geometry;
        if (stable >= 3) {
            gate.textContent = 'READY: 16/16 faces detected; negative control passed; layout stable for three frames. A pagination calculation could now begin. Elapsed ' + (Date.now() - start) + ' ms.';
            gate.className = 'ready'; stopped = true; cleanup(); return;
        }
        gate.textContent = 'Measurement gate CLOSED: ' + count + '/16 faces detected. Waiting for all fonts and stable layout.';
        if (Date.now() - start >= 30000) {
            gate.textContent = 'INCONCLUSIVE: ' + count + '/16 faces detected within the diagnostic limit. Gate stays CLOSED. Check Diagnostics and Reload; do not assume fallback is the requested font.';
            gate.className = 'failed'; stopped = true; cleanup(); return;
        }
        frame(tick);
    }
    tick();
}());
