/*
 * Gotest question content renderer for the Android WebView (T-026 criterion 6).
 * A port of resources/js/Components/MathContent.tsx on the web: same KaTeX version (0.18.7), same
 * inline-math / block-math hydration, same leftover `$...$` / `$$...$$` handling and the same
 * "plain text or HTML" detection, so a question looks the same in the app and on gotest.id.
 *
 * The app talks to this page through the `GotestBridge` web message listener (androidx.webkit):
 *   app  -> page: GotestRich.render(content, isHtml, baseUrl, style) and GotestRich.tapAt(x, y)
 *   page -> app : {"type":"height","value":<css px>} and {"type":"image","src":"<url>"}
 * Links never navigate: the WebView takes no touches and refuses every navigation.
 */
(function () {
    'use strict';

    function escapeHtml(value) {
        return value
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function renderLatex(latex, displayMode) {
        try {
            return katex.renderToString(latex, { throwOnError: false, displayMode: displayMode });
        } catch (e) {
            return escapeHtml(latex);
        }
    }

    function renderDollarMath(text) {
        if (!text) {
            return '';
        }
        return text
            .split(/(\$\$[\s\S]+?\$\$|\$[^$]+\$)/g)
            .map(function (part) {
                if (part.startsWith('$$') && part.endsWith('$$') && part.length >= 4) {
                    return renderLatex(part.slice(2, -2), true);
                }
                if (part.startsWith('$') && part.endsWith('$') && part.length >= 2) {
                    return renderLatex(part.slice(1, -1), false);
                }
                return escapeHtml(part);
            })
            .join('');
    }

    function hydrateMathNodes(root) {
        root.querySelectorAll('[data-type="inline-math"], [data-type="block-math"]').forEach(function (element) {
            var latex = element.getAttribute('data-latex') || '';
            var displayMode = element.getAttribute('data-type') === 'block-math';
            try {
                katex.render(latex, element, { throwOnError: false, displayMode: displayMode });
            } catch (e) {
                element.textContent = latex;
            }
        });

        var walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
            acceptNode: function (node) {
                if (!node.nodeValue || node.nodeValue.indexOf('$') === -1) {
                    return NodeFilter.FILTER_REJECT;
                }
                if (node.parentElement && node.parentElement.closest('.katex, [data-type="inline-math"], [data-type="block-math"]')) {
                    return NodeFilter.FILTER_REJECT;
                }
                return NodeFilter.FILTER_ACCEPT;
            },
        });

        var textNodes = [];
        while (walker.nextNode()) {
            textNodes.push(walker.currentNode);
        }

        textNodes.forEach(function (textNode) {
            var value = textNode.nodeValue || '';
            var html = renderDollarMath(value);
            if (html === escapeHtml(value)) {
                return;
            }
            var wrapper = document.createElement('span');
            wrapper.innerHTML = html;
            textNode.replaceWith.apply(textNode, Array.from(wrapper.childNodes));
        });
    }

    function containsHtml(value) {
        return /<\/?(p|br|strong|b|em|i|u|s|sub|sup|ul|ol|li|h2|h3|blockquote|a|mark|hr|span|div|img)\b/i.test(value);
    }

    function post(message) {
        if (window.GotestBridge) {
            window.GotestBridge.postMessage(JSON.stringify(message));
        }
    }

    var lastHeight = -1;

    function reportHeight() {
        var height = Math.ceil(document.getElementById('content').getBoundingClientRect().height);
        if (height !== lastHeight) {
            lastHeight = height;
            post({ type: 'height', value: height });
        }
    }

    /** Relative `/storage/...` images are joined with the server address, as on the web. */
    function absolutizeImages(root, baseUrl) {
        root.querySelectorAll('img').forEach(function (img) {
            var src = img.getAttribute('src') || '';
            if (baseUrl && src.charAt(0) === '/' && src.charAt(1) !== '/') {
                img.setAttribute('src', baseUrl.replace(/\/$/, '') + src);
            }
            img.addEventListener('load', reportHeight);
            img.addEventListener('error', reportHeight);
        });
    }

    function applyStyle(style) {
        if (!style) {
            return;
        }
        var root = document.documentElement.style;
        Object.keys(style).forEach(function (key) {
            root.setProperty('--' + key, style[key]);
        });
    }

    /**
     * The WebView takes no touches (so a tap on an option still selects it and the screen still scrolls),
     * so a formula wider than the screen cannot be scrolled sideways: shrink it to fit instead.
     */
    function fitWideMath(root) {
        root.querySelectorAll('.katex-display, [data-type="inline-math"]').forEach(function (element) {
            element.style.fontSize = '';
            var available = element.clientWidth;
            var needed = element.scrollWidth;
            if (available > 0 && needed > available) {
                element.style.fontSize = Math.max(55, Math.floor((available / needed) * 100)) + '%';
            }
        });
    }

    window.addEventListener('resize', function () {
        fitWideMath(document.getElementById('content'));
        reportHeight();
    });

    window.GotestRich = {
        render: function (raw, isHtml, baseUrl, style) {
            applyStyle(style);
            raw = raw || '';
            var target = document.getElementById('content');
            target.innerHTML = isHtml || containsHtml(raw) ? raw : renderDollarMath(raw);
            absolutizeImages(target, baseUrl);
            hydrateMathNodes(target);
            fitWideMath(target);
            lastHeight = -1;
            reportHeight();
            if (document.fonts && document.fonts.ready) {
                document.fonts.ready.then(function () {
                    fitWideMath(target);
                    reportHeight();
                });
            }
        },

        /** The app passes a tap position (CSS px); an image under it opens full screen in the app. */
        tapAt: function (x, y) {
            var element = document.elementFromPoint(x, y);
            var img = element && element.closest ? element.closest('img') : null;
            if (img) {
                post({ type: 'image', src: img.currentSrc || img.src });
            }
        },
    };
})();
