/**
 * Module that will permit to hide et show content with a toggle button 
 * this module comes with the style file toggle.css
 * 
 * @author Solofo R.
 * 
 */

// get all button with class "toggle-btn" and add event listener to toggle 
// all element with class "toggle-content"
const toggleBtns = document.querySelectorAll(".toggle-btn");
toggleBtns.forEach((btn) => {
    btn.addEventListener("mousedown", function(e) {
        const currentBtn = e.target;
        const btnId = currentBtn.id;
        // get the element to toggle identified by 'content-${btnId}'
        const eltToToggle = document.querySelector("#content-" + btnId);
        // if the element is hidden
        if (eltToToggle.style.display != "block") {
            // show the element 
            eltToToggle.style.display = "block";
            // hide the first icon inside the button and show the last one
            currentBtn.firstElementChild.style.display = "none";
            currentBtn.lastElementChild.style.display = "inline";
        }
        else {
            // show the element 
            eltToToggle.style.display = "none";
            // show the first icon inside the button and hide the last one
            currentBtn.firstElementChild.style.display = "inline";
            currentBtn.lastElementChild.style.display = "none";
        }
    });
});

