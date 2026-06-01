package com.mycompany.app;

import java.nio.ByteBuffer;

// Protocol format
// Identificador: IX
// Operações
// P: Packet with page
// R: Request for page
// M: Mutate checkbox
// U: Checkbox updated
// Delimitador: ?
// Exemplos: 
// IXP1000?<Counteũdo> - Enviando a página 1000;
// IXR1001 - Requisitando a página 1001;
// IXM1000?1 - Atualizar a checkbox 1000 para true;
// IXM1000?0 - Atualizar a checkbox 1000 para false;
// IXU1000?1 - Notificação de que a checkbox 1000 foi atualizada para verdadeiro
// IXU1000?0 - Notificação de que a checkbox 1000 foi atualizada para falso;

enum ProtocolOperation {
    PAGE("P"),
    REQUEST("R"),
    MUTATE("M"),
    UPDATE("U");

    private final String identificator;

    ProtocolOperation(String id) {
        this.identificator = id;
    }

    @Override
    public String toString() {
        return this.identificator;
    }

    public byte toByte() {
        return this.identificator.getBytes()[0];
    }

}

abstract class ProtocolMessage {
    public final ProtocolOperation operation;
    public final String protocolStart = "IX";
    public final String delimiter = "?";

    public ProtocolMessage(ProtocolOperation operation) {
        this.operation = operation;
    }

    abstract byte[] getRaw();
}

class ProtocolPageMessage extends ProtocolMessage {
    public final Integer page;
    public final byte[] content;

    public ProtocolPageMessage(Integer page, byte[] content) {
        super(ProtocolOperation.PAGE);
        this.page = page;
        this.content = content;
    }

    @Override
    byte[] getRaw() {
        // IXP1000?<Counteúdo> - Enviando a página 1000;
        String rawHead = this.protocolStart + this.operation.toString() + this.page + this.delimiter;
        byte[] rawHeadBytes = rawHead.getBytes();

        return ByteBuffer
                .allocate(rawHeadBytes.length + this.content.length)
                .put(rawHeadBytes)
                .put(this.content)
                .array();
    }
}

class ProtocolMutatedMessage extends ProtocolMessage {
    public final Integer checkboxId;
    public final boolean value;

    public ProtocolMutatedMessage(Integer checkboxId, boolean value) {
        super(ProtocolOperation.MUTATE);
        this.checkboxId = checkboxId;
        this.value = value;
    }

    @Override
    byte[] getRaw() {
        // IXM1000?1 - Atualizar a checkbox 1000 para true;
        String rawHead = this.protocolStart + this.operation.toString() + this.checkboxId + this.delimiter;
        byte[] rawHeadBytes = rawHead.getBytes();
        byte value = (byte)(this.value ? 1 : 0);

        return ByteBuffer
                .allocate(rawHeadBytes.length + 1)
                .put(rawHeadBytes)
                .put(value)
                .array();
    }
}

public class ProtocolService {
    public ProtocolService() {
    }

    // public ProtocolMessage parseEncoded(ByteBuffer blob) throws Exception {
    // byte[] message = new byte[blob.remaining()];
    // blob.get(message);

    // // Validate protocol
    // if (message[0] != 'I' || message[1] != 'X') {
    // throw new Exception("Protocol mismatch");
    // }

    // if (message[2] == ProtocolOperation.PAGE.toByte()) {
    // return this.parsePageOperation(blob);
    // }

    // throw new Exception("Failed to parse message");
    // }

    // private ProtocolPageMessage parsePageOperation(ByteBuffer blob) throws
    // Exception {
    // return null;
    // }

    // public ProtocolMessage parseEncodedBefore(ByteBuffer blob) throws Exception {
    // byte[] message = new byte[blob.remaining()];
    // blob.get(message);

    // int delimiterIndex = -1;

    // // Find S delimiter
    // for (int i = 3; i < message.length; i++) {
    // if (message[i] == 'S') {
    // delimiterIndex = i;
    // break;
    // }
    // }

    // if (delimiterIndex == -1) {
    // throw new Exception("Missing status delimiter");
    // }

    // // Ensure status byte exists
    // if (delimiterIndex + 1 >= message.length) {
    // throw new Exception("Missing status byte");
    // }

    // // Extract checkbox id
    // String checkboxIdStr = new String(
    // message,
    // 3,
    // delimiterIndex - 3,
    // java.nio.charset.StandardCharsets.UTF_8);

    // int checkboxId;

    // try {
    // checkboxId = Integer.parseInt(checkboxIdStr);
    // } catch (NumberFormatException e) {
    // throw new Exception("Invalid checkbox id");
    // }

    // byte statusByte = message[delimiterIndex + 1];

    // if (statusByte != 0 && statusByte != 1) {
    // throw new Exception("Invalid status byte");
    // }

    // boolean checked = statusByte == 1;

    // System.out.println("Message parsed");
    // System.out.println("Checkbox number: " + checkboxId);
    // System.out.println("Checkbox status: " + checked);

    // return new ProtocolDecodedMessage(checkboxId, checked, new String(message));
    // }

}
