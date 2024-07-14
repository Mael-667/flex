
document.querySelector("#labelavatar").style.background = avatar;
document.querySelector("#username").innerText = username;

document.querySelector("#form").addEventListener("submit", async function(e){
    let form = e.target;
    let formdata = new FormData();
    formdata.append("name", username)
    formdata.append("password", form.password.value)
    const xhr = new XMLHttpRequest();
    xhr.open("POST", "/?req=login");
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(formdata);
    xhr.onreadystatechange = function() {
        if (this.readyState == XMLHttpRequest.DONE && this.status == 200) {
            localStorage.setItem("token", this.getResponseHeader("token"));
            token = this.getResponseHeader("token");
            document.querySelector("#form").removeEventListener("submit",function(e){});
            loadHtml(document.body, xhr.response)
        } else if(this.readyState == XMLHttpRequest.DONE && this.status == 418){
            let span = document.createElement("span")
            span.id = "error"
            span.textContent = "Mot de passe invalide"
            span.style = "color: red;position: relative;top: -0.7em;"
            let target = document.querySelector("#password");
            target.insertAdjacentElement("afterend", span) 
            target.addEventListener("focus", () => {
                document.querySelector("#error").remove();
                target.removeEventListener("focus", () =>{})
            })
        }
    }
})
