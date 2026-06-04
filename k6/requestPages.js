import ws from "k6/ws"


export const options = {
    iterations: 10000,
    vus: 1000
}

export default function () {
    const url = "ws://127.0.0.1:6969"
    const interval = Math.random() * 1000
    const duration = 10000;
    const ITEMS_PER_PAGE = 2000;

    const res = ws.connect(url, undefined, function (socket) {
        socket.on("open", () => {
            console.log("Connected")
        })

        socket.setInterval(() => {
            const PAGE = Math.round(Math.random() * 1000)
            console.log({ PAGE })
            socket.sendBinary(new Uint8Array(
                ("IXR" + PAGE + "?" + ITEMS_PER_PAGE).split("").map(c => c.charCodeAt(0))
            ).buffer)
        }, interval)

        socket.on("binaryMessage", (message) => {
            const view =  new Uint8Array(message);

            console.log("Received " + view.byteLength + " bytes")

        })

        socket.setTimeout(() => {
            console.log("Closing connection");
            socket.close();
        }, duration);
    })
}

