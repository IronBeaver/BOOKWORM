/* Trusted, read-only input guard. Does not change BW pagination, DOM content or positions. ES5. */
(function () {
    'use strict';
    window.BWInput = {
        hit: function (relativeX, relativeY) {
            var selected = window.getSelection ? window.getSelection() : null;
            var selection = !!(selected && selected.rangeCount && !selected.isCollapsed);
            var node = document.elementFromPoint(relativeX * window.innerWidth, relativeY * window.innerHeight);
            if (!node) return {interactive: true, selection: selection};
            while (node && node.nodeType === 1) {
                var tag = node.tagName.toLowerCase();
                if ((tag === 'a' && node.hasAttribute('href')) || tag === 'button' || tag === 'input' ||
                        tag === 'textarea' || tag === 'select' || node.isContentEditable) {
                    return {interactive: true, selection: selection};
                }
                node = node.parentNode;
            }
            return {interactive: false, selection: selection};
        }
    };
}());
