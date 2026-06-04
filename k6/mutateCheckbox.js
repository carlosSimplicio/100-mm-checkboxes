import { WebSocket } from "k6/websockets"

export const options = {
    vus: 1000,
    iterations: 10000
}

let timeoutId;
let intervalId;
export default function () {
    const url = "ws://127.0.0.1:6969"
    const ws = new WebSocket(url)

    ws.addEventListener("open", () => {
        console.log("Connected")
        const interval = Math.random() * 1000

        intervalId = setInterval(() => {
            const CHECKBOXID = Math.round(Math.random() * 2_000)
            const value = Math.random() > 0.5
            ws.send(new Uint8Array([
                ...("IXM" + CHECKBOXID + "?").split("").map((c) => c.charCodeAt(0)),
                value ? 1 : 0
            ]))
        }, interval)

        timeoutId = setTimeout(() => {
            clearInterval(intervalId)
            console.log("Closing the connection")
            ws.close()

        }, 10000)
    })

}
