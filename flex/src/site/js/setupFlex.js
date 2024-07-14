xhr = new XMLHttpRequest();
    xhr.open("GET", "/?req=getDefaultFolder");
    xhr.setRequestHeader("token", token);
    xhr.send();
    xhr.responseType = "text";
    xhr.onload = () => {
        if (xhr.readyState == 4 && xhr.status == 200) {
            document.querySelector(".conteneur").innerHTML=displayFiles(xhr.response);
        } else {
            console.log(`Error: ${xhr.status}`);
        }
    }

function setupflex(){
    let path = document.querySelector(".dossselec").id;
    if(path != ""){
        const xhr = new XMLHttpRequest();
        xhr.open("GET", "/?req=setupFlex&path="+encodeURI(path));
        xhr.setRequestHeader("token", token);
        xhr.send();
        xhr.responseType = "text";
        xhr.onload = () => {
            if (xhr.readyState == 4 && xhr.status == 200) {
                //document.querySelector(".conteneur").innerHTML+=displayFiles(xhr.response);
            } else {
                console.log(`Error: ${xhr.status}`);
            }
        }
    }
}

function setFolder(folder){
    document.querySelector(".dossselec").childNodes[0].nodeValue = folder.id;
    document.querySelector(".dossselec").id = folder.id;
}


function getFiles(path){
    const xhr = new XMLHttpRequest();
    xhr.open("GET", "/?req=getFolder&path="+encodeURI(path));
    xhr.setRequestHeader("token", token);
    xhr.send();
    xhr.responseType = "text";
    xhr.onload = () => {
        if (xhr.readyState == 4 && xhr.status == 200) {
            document.querySelector(".conteneur").innerHTML=displayFiles(xhr.response);
        } else {
            console.log(`Error: ${xhr.status}`);
        }
    }
}