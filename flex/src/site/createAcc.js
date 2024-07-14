document.querySelector("#form").addEventListener("submit", async function(e){
    let form = e.target;
    let formdata = new FormData();
    formdata.append("name", form.name.value);
    formdata.append("password", form.password.value);
    formdata.append("avatar", form.avatar.files[0]);
    console.log(formdata);
    const xhr = new XMLHttpRequest();
    xhr.open("POST", "/?req=createAcc");
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(formdata);
    xhr.onreadystatechange = function() {
        if (this.readyState == XMLHttpRequest.DONE && this.status == 200) {
            localStorage.setItem("token", this.getResponseHeader("token"));
            token = this.getResponseHeader("token");
            document.querySelector("#form").removeEventListener("submit",function(e){});
            loadHtml(document.body, xhr.response);
        } else if(this.readyState == XMLHttpRequest.DONE && this.status == 403){
            let span = document.createElement("span")
            span.id = "error"
            span.textContent = "Nom déjà pris"
            span.style = "color: red;position: relative;top: -0.7em;"
            document.querySelector("#name").insertAdjacentElement("afterend", span) 
            document.querySelector("#name").addEventListener("focus", () => {
                document.querySelector("#error").remove();
                document.querySelector("#name").removeEventListener("focus", () =>{})
            })
        }
    }
})



function change(elmt){
    console.log(elmt.files[0]);
    document.querySelector("#labelavatar").style = `background: url(${URL.createObjectURL(elmt.files[0])}); !important`;
    document.querySelector("#avatarinput").style = "opacity: 0;";
}
