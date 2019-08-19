
    //Override mimicMouseDown_
    zk.Widget.mimicMouseDown_ = function (wgt, noFocusChange, which) { 
        var modal = zk.currentModal;
        
        //Skip modal with noautofocus = true
        if(modal && modal.domExtraAttrs && modal.domExtraAttrs.noautofocus == 'true') return;
        
        if (modal && !wgt) {
            var cf = zk.currentFocus;
            if (cf && zUtl.isAncestor(modal, cf)) cf.focus(0);
            else modal.focus(0);
        } else if (!wgt || wgt.canActivate()) {
            if (!noFocusChange) {
                var wgtVParent;
                zk._prevFocus = zk.currentFocus;
                zk.currentFocus = wgt;
                if (wgt && (wgtVParent = wgt.$n('a')) && jq.nodeName(wgtVParent, 'button', 'input', 'textarea', 'a', 'select', 'iframe')) {
                    
                    var oldStyle;
                    if (zk.ie) {
                        oldStyle = wgtVParent.style.position;
                        wgtVParent.style.position = 'fixed';
                    }
                    wgt.focus();
                    if (zk.ie) wgtVParent.style.position = oldStyle;
                }
                zk._cfByMD = true;
                setTimeout(function () {zk._cfByMD = false; zk._prevFocus = null;}, 0);
                    
            }

            if (wgt) 
                zWatch.fire('onFloatUp', wgt, {triggerByClick: which}); 
            else
                for (var dtid in zk.Desktop.all)
                    zWatch.fire('onFloatUp', zk.Desktop.all[dtid]); 
        }
    }
