package com.example.nexus.util;

import java.util.Locale;


public class WebViewJavaScriptInjector {

    private WebViewJavaScriptInjector() {
        
    }

    
    public static String getDownloadBridgeInjection() {
        return """
            (function() {
                if (window.nexusDownloadBridgeInstalled) return;
                window.nexusDownloadBridgeInstalled = true;
                
                if (!window.nexusDownloadBridge) {
                    console.warn('NexusDownloadBridge: Bridge object not found');
                    return;
                }
                console.log('NexusDownloadBridge: Installing download interceptor');
                
                // Global click interceptor
                document.addEventListener('click', function(event) {
                    try {
                        var el = event.target;
                        var depth = 0;
                        
                        while (el && el !== document && depth < 15) {
                            // Check for download attribute
                            if (el.tagName === 'A' && el.hasAttribute('download') && el.href) {
                                console.log('NexusDownloadBridge: Download link clicked:', el.href);
                                event.preventDefault();
                                event.stopPropagation();
                                window.nexusDownloadBridge.download(el.href);
                                return false;
                            }
                            
                            // Check for download-related data attributes
                            var attrs = ['data-download', 'data-download-url', 'data-url', 'data-href', 'data-src'];
                            for (var i = 0; i < attrs.length; i++) {
                                var attrVal = el.getAttribute(attrs[i]);
                                if (attrVal && attrVal.match(/^https?:\\/\\//)) {
                                    console.log('NexusDownloadBridge: Found download URL in', attrs[i], ':', attrVal);
                                    window.nexusDownloadBridge.download(attrVal);
                                    event.preventDefault();
                                    event.stopPropagation();
                                    return false;
                                }
                            }
                            
                            // Check for download button text
                            var text = (el.textContent || '').toLowerCase().trim();
                            var ariaLabel = (el.getAttribute('aria-label') || '').toLowerCase();
                            if ((text === 'download' || text.startsWith('download ') || ariaLabel.includes('download')) &&
                                (el.tagName === 'BUTTON' || el.tagName === 'A' || el.getAttribute('role') === 'button')) {
                                
                                console.log('NexusDownloadBridge: Download button clicked');
                                
                                var img = el.querySelector('img') || el.closest('div').querySelector('img');
                                if (img && img.src && img.src.match(/^https?:\\/\\//)) {
                                    console.log('NexusDownloadBridge: Found image:', img.src);
                                    window.nexusDownloadBridge.download(img.src);
                                    event.preventDefault();
                                    event.stopPropagation();
                                    return false;
                                }
                                
                                var link = el.closest('a') || el.parentElement.querySelector('a');
                                if (link && link.href && link.href.match(/^https?:\\/\\//)) {
                                    console.log('NexusDownloadBridge: Found link:', link.href);
                                    window.nexusDownloadBridge.download(link.href);
                                    event.preventDefault();
                                    event.stopPropagation();
                                    return false;
                                }
                            }
                            
                            if (el.tagName === 'IMG' && el.src && el.src.match(/^https?:\\/\\//)) {
                                var parent = el.parentElement;
                                if (parent && (parent.hasAttribute('download') || 
                                              parent.getAttribute('data-download') || 
                                              (parent.textContent || '').toLowerCase().includes('download'))) {
                                    console.log('NexusDownloadBridge: Image in download context:', el.src);
                                    window.nexusDownloadBridge.download(el.src);
                                    event.preventDefault();
                                    event.stopPropagation();
                                    return false;
                                }
                            }
                            
                            el = el.parentElement;
                            depth++;
                        }
                    } catch (e) {
                        console.error('NexusDownloadBridge error:', e);
                    }
                }, true);
                
                console.log('NexusDownloadBridge: Download interceptor installed');
            })();
            """;
    }

    
    public static String getFaviconExtractionScript() {
        return """
            (function() {
                var link = document.querySelector("link[rel*='icon']");
                if (link) {
                    return link.href;
                }
                return window.location.origin + '/favicon.ico';
            })();
            """;
    }

    
    public static String getContextMenuBlockScript() {
        return """
            document.addEventListener('contextmenu', function(e) {
                e.preventDefault();
            }, true);
            """;
    }

    
    public static String getHoveredElementScript() {
        return """
            (function() {
                var elem = document.elementFromPoint(arguments[0], arguments[1]);
                return elem ? elem.tagName : '';
            })();
            """;
    }

    
    public static String getImageSrcAtPointScript(double x, double y) {
        return String.format(Locale.US,
            "(function() { var el = document.elementFromPoint(%f, %f); " +
            "if (el && el.tagName === 'IMG') return el.src; " +
            "if (el && el.tagName === 'A') { var img = el.querySelector('img'); if (img) return img.src; } " +
            "return null; })();", x, y);
    }

    
    public static String getLinkHrefAtPointScript(double x, double y) {
        return String.format(Locale.US,
            "(function() { var el = document.elementFromPoint(%f, %f); " +
            "while (el && el.tagName !== 'A') el = el.parentElement; " +
            "return el ? el.href : null; })();", x, y);
    }

    
    public static String getCopyTextScript(String text) {
        String escaped = text.replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r");
        return String.format(
            "navigator.clipboard.writeText(\"%s\").catch(function(e) { console.error('Copy failed:', e); });",
            escaped);
    }

    
    public static String getDarkModeScript() {
        return """
            (function() {
                if (document.getElementById('nexus-dark-mode-style')) return;
                var style = document.createElement('style');
                style.id = 'nexus-dark-mode-style';
                style.textContent = 'html { filter: invert(1) hue-rotate(180deg); } img, video { filter: invert(1) hue-rotate(180deg); }';
                document.head.appendChild(style);
            })();
            """;
    }

    
    public static String getDisableDarkModeScript() {
        return """
            (function() {
                var style = document.getElementById('nexus-dark-mode-style');
                if (style) style.remove();
            })();
            """;
    }

    
    public static String getScrollToScript(double x, double y) {
        return String.format(Locale.US, "window.scrollTo(%f, %f);", x, y);
    }

    
    public static String getScrollPositionScript() {
        return "JSON.stringify({ x: window.scrollX || window.pageXOffset, y: window.scrollY || window.pageYOffset });";
    }

    
    public static String extractAllImagesScript() {
        return """
            (function() {
                var images = Array.from(document.querySelectorAll('img'))
                    .map(img => img.src)
                    .filter(src => src && src.startsWith('http'));
                return JSON.stringify(images);
            })();
            """;
    }
}

