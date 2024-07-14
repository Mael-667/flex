function getBraveSetup(){
    const xhr = new XMLHttpRequest();
    xhr.open("GET", "/?req=getBraveSetup");
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

function goToMain(){
    const xhr = new XMLHttpRequest();
    xhr.open("GET", "/?req=main");
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