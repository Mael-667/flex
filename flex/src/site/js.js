let token = localStorage.getItem("token");

let xhr = new XMLHttpRequest();
    xhr.open("GET", "/?req=getProfiles");
    xhr.send();
    xhr.responseType = "text";
    xhr.onload = () => {
        if (xhr.readyState == 4 && xhr.status == 200) {
            let profiles = JSON.parse(xhr.response);
            let conteneur = document.querySelector("#profils")
            let btn = conteneur.innerHTML;
            let div = "";
            profiles.forEach((profil) => {
                div += `<div class="conteneurprofil" onclick="login(this)"><div class="profil" style="background: url(${profil.avatar});"></div><div class="name">${profil.name}</div></div>`
            })
            conteneur.innerHTML = div+btn;
        } else {
            console.log(`Error: ${xhr.status}`);
        }
    }


let avatar;
let username;
function login(elmt){
    avatar = elmt.firstChild.style.background;
    username = elmt.querySelector(".name").innerText;
    let xhr = new XMLHttpRequest();
        xhr.open("GET", "/?req=login");
        xhr.send();
        xhr.responseType = "text";
        xhr.onload = () => {
            if (xhr.readyState == 4 && xhr.status == 200) {
                loadHtml(document.body, xhr.response);
            } else {
                console.log(`Error: ${xhr.status}`);
            }
        }
}

function createProfil(elmt){
    if(document.querySelector("#profils").childNodes.length < 15){
        const xhr = new XMLHttpRequest();
        xhr.open("GET", "/?req=createAcc");
        xhr.send();
        xhr.responseType = "text";
        xhr.onload = () => {
            if (xhr.readyState == 4 && xhr.status == 200) {
                loadHtml(document.body, xhr.response);
            } else {
                console.log(`Error: ${xhr.status}`);
            }
        }
    } else {
        elmt.nextElementSibling.innerText = "Limite de profil atteinte";
        elmt.nextElementSibling.style = "color: red;"
    }
};

function getFlexSetup(){
    const xhr = new XMLHttpRequest();
    xhr.open("GET", "/?req=getFlexSetup");
    xhr.setRequestHeader("token", token);
    xhr.send();
    xhr.responseType = "text";
    xhr.onload = () => {
        if (xhr.readyState == 4 && xhr.status == 200) {
            loadHtml(document.body, xhr.response);
        } else {
            console.log(`Error: ${xhr.status}`);
        }
    }
};


let retour = "/";





function loadHtml(destination, html){
    destination.innerHTML = html;
    let src = destination.querySelector("script").src;
    loadScript(src);
}


function loadScript(url){    
    var head = document.getElementsByTagName('head')[0];
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = url;
    head.appendChild(script);
}

function utf8(string){
    const reader = new FileReader()
    reader.readAsText(string, "UTF-8")
    reader.addEventListener("loadend", v => {
        return reader.result;
    })
}

function displayFiles(xhr){
    let files = JSON.parse(xhr);
    let parent = files[0].url.split("/")
    parent = parent.slice(0, parent.length-2).join("/")
    let bruh = `<span class="getParent" ondblclick="getFiles('${parent}')">Dossier parent</span>`;
    files.forEach(file => {
        if(!file.file){
            let div = `<div class="file" id="${file.url}" ondblclick="if(${!file.file}){getFiles(this.id)}" onclick="if(${!file.file}){setFolder(this)}" ><span class="filename">${file.name}</span><img src="${file.minia}" class="miniafile"></div>`
            bruh+=div;
        }
    });
    files.forEach(file => {
        if(file.file){
            let div = `<div class="file" id="${file.url}" ondblclick="if(${!file.file}){getFiles(this.id)}" onclick="if(${!file.file}){setFolder(this)}" ><span class="filename">${file.name}</span><img src="${file.minia}" class="miniafile"></div>`
            bruh+=div;
        }
    });
    return bruh;
}