/* Trusted ES5 font barrier. Does not change chapter text, input or pagination. */
var BWFonts = (function () {
    'use strict';
    var result = {state: 'pending', faces: 0}, token = 0, nodes = [];
    var sample = 'mmmmmmmmmmWWWWWWWWWWiiiiiiiiii0123456789 affinity office';
    var fallbacks = ['monospace', 'serif', 'sans-serif'];
    var frame = window.requestAnimationFrame || function (callback) { return window.setTimeout(callback, 16); };
    function cleanup() {
        for (var i = 0; i < nodes.length; i++) if (nodes[i].parentNode) nodes[i].parentNode.removeChild(nodes[i]);
        nodes = [];
    }
    function span(family, weight, style) {
        var node = document.createElement('span');
        node.setAttribute('aria-hidden', 'true');
        node.style.cssText = 'position:absolute;left:-20000px;top:0;visibility:hidden;white-space:nowrap;font-size:48px;line-height:normal;';
        // The chapter's universal max-width rule must not clamp measurement probes.
        node.style.setProperty('max-width', 'none', 'important');
        node.style.fontFamily = family; node.style.fontWeight = weight; node.style.fontStyle = style;
        node.textContent = sample; document.body.appendChild(node); nodes.push(node); return node;
    }
    function begin(family) {
        var ticket = ++token;
        cleanup(); result = {state: family ? 'pending' : 'ready', faces: 0};
        if (!family) return;
        var probes = [], start = Date.now(), stable = 0, previous = '';
        for (var face = 0; face < 4; face++) {
            var weight = face >= 2 ? '700' : '400', style = face % 2 ? 'italic' : 'normal';
            var p = {test: [], base: []};
            for (var i = 0; i < fallbacks.length; i++) {
                p.base.push(span(fallbacks[i], weight, style));
                p.test.push(span(family + ',' + fallbacks[i], weight, style));
            }
            probes.push(p);
        }
        function tick() {
            if (ticket !== token) return;
            var count = 0, widths = [];
            for (var f = 0; f < probes.length; f++) {
                var p = probes[f], width = p.test[0].getBoundingClientRect().width;
                var matches = width > 0, different = false;
                for (var i = 0; i < fallbacks.length; i++) {
                    if (Math.abs(p.test[i].getBoundingClientRect().width - width) > 0.1) matches = false;
                    if (Math.abs(p.base[i].getBoundingClientRect().width - width) > 0.5) different = true;
                }
                if (matches && different) count++;
                widths.push(width);
            }
            var geometry = widths.join(',') + ':' + document.documentElement.clientWidth + ':' + document.body.scrollHeight;
            stable = count === 4 && geometry === previous ? stable + 1 : 0;
            previous = geometry; result.faces = count;
            if (stable >= 3) { result.state = 'ready'; cleanup(); return; }
            // Diagnostic limit fails closed; it is never treated as font readiness.
            if (Date.now() - start >= 30000) { result.state = 'failed'; cleanup(); return; }
            frame(tick);
        }
        tick();
    }
    return {begin: begin, status: function () { return {state: result.state, faces: result.faces}; }};
}());
