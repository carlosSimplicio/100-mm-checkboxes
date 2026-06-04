const ITEMS_PER_PAGE = 1000;
const TOTAL_CHECKBOXES = 1_000_000;
const MAX_PAGE_NUMBER = TOTAL_CHECKBOXES / ITEMS_PER_PAGE;
const WEBSOCKET_URL = "ws://127.0.0.1:6969"

let websocket;
let minShowing = 0;
let maxShowing = 0;

const colors = ["yellow", "pink", "cyan"]

function requestPage(pageNumber) {
    if (pageNumber < 1 || pageNumber > (MAX_PAGE_NUMBER)) return;

    // IXR1001 - Requisitando a página 1001;
    const message = new Uint8Array([
        ..."IXR".split("").map(c => c.charCodeAt(0)),
        ...pageNumber.toString().split("").map(c => c.charCodeAt(0)),
        "?".charCodeAt(0),
        ...ITEMS_PER_PAGE.toString().split("").map(c => c.charCodeAt(0)),
    ])

    console.log(new TextDecoder().decode(message))
    websocket.send(message);
}


function removePage(pageNumber) {
    if (pageNumber !== maxShowing && pageNumber !== minShowing) return;

    document.querySelectorAll(`[data-page-id='${pageNumber}']`).forEach(el => el.remove())

    if (pageNumber === minShowing) minShowing++;
    if (pageNumber === maxShowing) maxShowing--;
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

function createCheckbox(id, checked, pageNumber) {
    const checkbox = document.createElement("input")
    checkbox.type = "checkbox"
    checkbox.id = id
    checkbox.checked = checked
    checkbox.dataset.pageId = pageNumber;
    const selectedColor = colors[(pageNumber - 1) % colors.length]
    checkbox.style = "accent-color: " + selectedColor
    return checkbox;
}

function createTitle(pageNumber) {
    const title = document.createElement("h1");
    title.textContent = "100 MILLION CHECKBOXES";
    title.id = "title"
    title.dataset.pageId = pageNumber;

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
    let count = (pageNumber - 1) * ITEMS_PER_PAGE;
    outer:
    for (let i = delimiterIndex + 1; i < uint8Array.length; i++) {
        const b = uint8Array[i];
        checkboxes.push(
            createCheckbox(count++, (b & 128) !== 0, pageNumber)
        )
        checkboxes.push(
            createCheckbox(count++, (b & 64) !== 0, pageNumber)
        )
        checkboxes.push(
            createCheckbox(count++, (b & 32) !== 0, pageNumber)
        )
        checkboxes.push(
            createCheckbox(count++, (b & 16) !== 0, pageNumber)
        )
        checkboxes.push(
            createCheckbox(count++, (b & 8) !== 0, pageNumber)
        )
        checkboxes.push(
            createCheckbox(count++, (b & 4) !== 0, pageNumber)
        )
        checkboxes.push(
            createCheckbox(count++, (b & 2) !== 0, pageNumber)
        )
        checkboxes.push(
            createCheckbox(count++, (b & 1) !== 0, pageNumber)
        )
    }

    const page = checkboxes;
    if (pageNumber === 1) {
        page.push(createTitle(pageNumber))
    }

    const mainContainer = document.getElementById("main");
    // ADJUST THIS SO PAGES GET INSERTED ALWAYS IN THE RIGHT POSITION INDEPENDENT OF RESPONSE ARRIVAL;
    if (pageNumber === 1 && minShowing === 0) {
        minShowing++;
        maxShowing++
        mainContainer.prepend(...page);
    }
    else if (pageNumber > maxShowing) {
        maxShowing++;
        mainContainer.append(...page);
    } else if (pageNumber < minShowing) {
        minShowing--;
        mainContainer.prepend(...page)
    }

    const observer = new IntersectionObserver((event) => {
        const { target, isVisible, isIntersecting, boundingClientRect } = event[0];
        const pageId = parseInt(target.dataset.pageId);

        if (isIntersecting && boundingClientRect?.y > 0) {
            requestPage(pageId + 2);
        } else if (isIntersecting && boundingClientRect?.y < 0) {
            requestPage(pageId - 2);
        } else if (!isIntersecting && boundingClientRect?.y > 0) {
            removePage(pageId + 2);
        } else if (!isIntersecting && boundingClientRect?.y < 0) {
            removePage(pageId - 2);
        }

    }, { rootMargin: `${0}px` })

    observer.observe(page[0])
}


document.addEventListener("DOMContentLoaded", (event) => {
    console.log("DOM is fully loaded and parsed. You can now safely manipulate elements.");

    websocket = new WebSocket(WEBSOCKET_URL)

    websocket.addEventListener("open", () => {
        console.log("Connected");
        requestPage(1);
        requestPage(2);
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
