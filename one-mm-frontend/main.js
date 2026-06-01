let maxPageExhibited = 1;
let websocket;
let count = 0;

function requestNextPage() {
    maxPageExhibited++;

    // IXR1001 - Requisitando a página 1001;
    const message = new Uint8Array([
        ..."IXR".split("").map(c => c.charCodeAt(0)),
        ...maxPageExhibited.toString().split("").map(c => c.charCodeAt(0))
    ])

    console.log(new TextDecoder().decode(message))
    websocket.send(message);
}

function updateCheckBox(message) {
    const statusDelimiterIndex = message.findIndex(b => b === "S".charCodeAt(0))
    const checkboxId = message.slice(3, statusDelimiterIndex);
    const checkboxIdDecoded = new TextDecoder().decode(checkboxId);
    const checked = message[statusDelimiterIndex + 1];

    const el = document.getElementById(checkboxIdDecoded);
    if (el) {
        el.checked = !!checked
    }

}

function findDelimiterIndex(message) {
    for (let index = 0; index < message.length; index++) {
        if (message[index] === "?".charCodeAt(0)) {
            return index;
        }
    }

    return -1;
}

function createPage(pageNumber) {
    const page = document.createElement("div");
    page.id = pageNumber;
    page.className = "page";

    return page;
}

function createCheckbox(id, checked) {
    const checkbox = document.createElement("input")
    checkbox.type = "checkbox"
    checkbox.id = id
    checkbox.checked = checked
    return checkbox;
}

function createTitle() {
    const title = document.createElement("h1");
    title.textContent = "ONE MILLION CHECKBOXES";
    title.id = "title"

    return title;
}

function addPage(uint8Array) {
    const delimiterIndex = findDelimiterIndex(uint8Array);
    if (delimiterIndex === -1) {
        console.log("Could not find delimiter index");
        return;
    }
    const pageNumber = Number.parseInt(new TextDecoder().decode(uint8Array.slice(3, delimiterIndex)));

    const checkboxes = [];
    outer:
    for (let i = delimiterIndex + 1; i < uint8Array.length; i++) {
        const b = uint8Array[i];
        checkboxes.push(
            createCheckbox(count++, (b & 128) !== 0)
        )
        checkboxes.push(
            createCheckbox(count++, (b & 64) !== 0)
        )
        checkboxes.push(
            createCheckbox(count++, (b & 32) !== 0)
        )
        checkboxes.push(
            createCheckbox(count++, (b & 16) !== 0)
        )
        checkboxes.push(
            createCheckbox(count++, (b & 8) !== 0)
        )
        checkboxes.push(
            createCheckbox(count++, (b & 4) !== 0)
        )
        checkboxes.push(
            createCheckbox(count++, (b & 2) !== 0)
        )
        checkboxes.push(
            createCheckbox(count++, (b & 1) !== 0)
        )
    }

    const toAppend = checkboxes;
    if (pageNumber === 1) {
        toAppend.push(createTitle())
    }
    const page = createPage(pageNumber);
    page.append(...toAppend);
    const mainContainer = document.getElementById("main");
    mainContainer.append(page);

    const observer = new IntersectionObserver((event) => {
        const { isIntersecting } = event[0];
        if (isIntersecting) {
            console.log("Loading next page");
            requestNextPage()
        }
    }, { rootMargin: "50%" })

    observer.observe(page)
}


document.addEventListener("DOMContentLoaded", (event) => {
    console.log("DOM is fully loaded and parsed. You can now safely manipulate elements.");

    const wsuri = "ws://127.0.0.1:6969"
    websocket = new WebSocket(wsuri)

    websocket.addEventListener("open", () => {
        console.log("Connected");
    })

    websocket.addEventListener("message", async (event) => {
        try {
            const ds = new DecompressionStream("gzip");
            const decompressedData = new Blob([event.data]).stream().pipeThrough(ds);
            const reader = decompressedData.getReader();
            while (true) {
                const result = await reader.read();

                if (result.done) break;

                const uint8Array = result.value;

                if (
                    uint8Array[0] !== "I".charCodeAt(0) &&
                    uint8Array[1] !== "X".charCodeAt(0)
                ) {
                    console.log("Failed to indentify protocol")
                    break;
                }

                if (uint8Array[2] !== "P".charCodeAt(0)) {
                    console.log("Operation not implemented yet")
                    break;
                }

                addPage(uint8Array);
            }
        } catch (err) {
            console.log(err)
            return
        }
    })

    websocket.addEventListener("close", () => {
        console.log("Disconnected")
    })

    document.addEventListener("input", (event) => {
        const checkboxId = Number(event.target.id);
        const status = event.target.checked;
        if (!Number.isInteger(checkboxId) || isNaN(checkboxId)) return;

        //Protocolo - IXM<CHECKBOX_NUMBER>?<STATUS>
        const dataToSend = new Uint8Array([
            ..."IXM".split("").map(c => c.charCodeAt(0)),
            ...checkboxId.toString().split("").map(c => c.charCodeAt(0)),
            "?".charCodeAt(0),
            Number(status)
        ]);

        websocket.send(dataToSend)
    })
});
