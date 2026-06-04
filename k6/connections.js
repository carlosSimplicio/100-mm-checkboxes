import ws from "k6/ws"


export const options = {
    iterations: 10000,
    vus: 100
}

export default function () {
    const url = "ws://127.0.0.1:6969"
    const duration = Math.random() * 1000

    const res = ws.connect(url, undefined, function (socket) {
        socket.on("open", () => {
            console.log("Connected")
        })

        socket.setTimeout(() => {
            console.log("Closing connection");
            socket.close();
        }, duration);
    })
}

