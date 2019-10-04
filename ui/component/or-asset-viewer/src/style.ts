import {css, unsafeCSS} from "lit-element";
import {DefaultColor1, DefaultColor2, DefaultColor5, DefaultColor3, DefaultColor4, DefaultBoxShadow, DefaultDisabledOpacity,
    DefaultBoxShadowBottom} from "@openremote/core";
import mdi from "@openremote/or-icon/dist/mdi-icons";

// language=CSS
export const style = css`

    :host {
        --internal-or-asset-viewer-background-color: var(--or-asset-viewer-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-asset-viewer-panel-margin: var(--or-asset-viewer-panel-margin, 30px);
        --internal-or-asset-viewer-panel-padding: var(--or-asset-viewer-panel-padding, 24px);
        --internal-or-asset-viewer-text-color: var(--or-asset-viewer-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-asset-viewer-title-text-color: var(--or-asset-viewer-title-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));       
        --internal-or-asset-viewer-panel-color: var(--or-asset-viewer-panel-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-asset-viewer-line-color: var(--or-asset-viewer-line-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));
        
        height: 100%;
        width: 100%;
        box-sizing: border-box;
        background-color: var(--internal-or-asset-viewer-background-color);
        padding: var(--internal-or-asset-viewer-panel-margin);
    }
   
    *[hidden] {
        display: none;
    }
    
    #container {
        box-sizing: border-box;
        height: 100%;
        display: grid;
        grid-template-columns: var(--internal-or-asset-viewer-grid-template-columns);
        grid-template-rows: var(--internal-or-asset-viewer-grid-template-rows);
        column-gap: var(--internal-or-asset-viewer-panel-margin);
        row-gap: var(--internal-or-asset-viewer-panel-margin);  
        -webkit-animation: fadein 0.3s; /* Safari, Chrome and Opera > 12.1 */
        -moz-animation: fadein 0.3s; /* Firefox < 16 */
        -ms-animation: fadein 0.3s; /* Internet Explorer */
        -o-animation: fadein 0.3s; /* Opera < 12.1 */
        animation: fadein 0.3s;
    }

    @keyframes fadein {
        from { opacity: 0; }
        to   { opacity: 1; }
    }

    /* Firefox < 16 */
    @-moz-keyframes fadein {
        from { opacity: 0; }
        to   { opacity: 1; }
    }

    /* Safari, Chrome and Opera > 12.1 */
    @-webkit-keyframes fadein {
        from { opacity: 0; }
        to   { opacity: 1; }
    }

    /* Internet Explorer */
    @-ms-keyframes fadein {
        from { opacity: 0; }
        to   { opacity: 1; }
    }

    /* Opera < 12.1 */
    @-o-keyframes fadein {
        from { opacity: 0; }
        to   { opacity: 1; }
    }
    
    .panel {
        display: flex;
        flex-direction: column;
        background-color: var(--internal-or-asset-viewer-panel-color);        
        margin: 0;
        padding: var(--internal-or-asset-viewer-panel-padding);
        box-shadow: ${unsafeCSS(DefaultBoxShadow)};
    }
    
    .panel-content-wrapper {
        flex: 1;
        height: 100%;
    }
    
    .panel-content {
        display: flex;
        flex-wrap: wrap;
        height: 100%;
    }
        
    .panel-title {
        text-transform: uppercase;
        font-weight: bolder;
        color: var(--internal-or-asset-viewer-title-text-color);
        margin-bottom: 12px;
        flex: 0 0 auto;
    }
   
    .field {
        margin: 10px 0;
        width: 100%;
        flex: 0 0 auto;
        min-height: 50px;
    }
    
    .field > * {
        width: 100%;
        box-sizing: border-box;
    }
    
    .msg {
        display: flex;
        justify-content: center;
        align-items: center;
        text-align: center;
        height: 100%;
    }    
`;