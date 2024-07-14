document.querySelector("#form").addEventListener("submit", function(e){
    let form = e.target;
    let formdata = new FormData();
    formdata.append("token", form.token.value)
    xhr = new XMLHttpRequest();
    xhr.open("POST", "/?req=setupBrave");
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(formdata);
    xhr.onreadystatechange = function() {
        if (this.readyState == XMLHttpRequest.DONE && this.status == 200) {
            getFlexSetup();
        }
    }
})
