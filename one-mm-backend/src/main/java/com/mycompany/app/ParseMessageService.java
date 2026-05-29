package com.mycompany.app;

import java.nio.ByteBuffer;

class ParsedMessage {
    public final Integer CheckboxId;
    public final boolean Value;
    public final String Raw;

    public ParsedMessage(Integer checkboxId, boolean value, String raw) {
        this.CheckboxId = checkboxId;
        this.Value = value;
        this.Raw = raw;
    }
}

public class ParseMessageService {
    public ParseMessageService() {
    }

    public ParsedMessage Parse(ByteBuffer blob) throws Exception {
        byte[] message = new byte[blob.remaining()];
        blob.get(message);

        // Minimum valid message:
        // IXM + checkbox id + S + 1 status
        if (message.length < 6) {
            throw new Exception("Message too short");
        }

        // Validate protocol
        if (message[0] != 'I' ||
                message[1] != 'X' ||
                message[2] != 'M') {

            throw new Exception("Protocol mismatch");
        }

        int delimiterIndex = -1;

        // Find S delimiter
        for (int i = 3; i < message.length; i++) {
            if (message[i] == 'S') {
                delimiterIndex = i;
                break;
            }
        }

        if (delimiterIndex == -1) {
            throw new Exception("Missing status delimiter");
        }

        // Ensure status byte exists
        if (delimiterIndex + 1 >= message.length) {
            throw new Exception("Missing status byte");
        }

        // Extract checkbox id
        String checkboxIdStr = new String(
                message,
                3,
                delimiterIndex - 3,
                java.nio.charset.StandardCharsets.UTF_8);

        int checkboxId;

        try {
            checkboxId = Integer.parseInt(checkboxIdStr);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid checkbox id");
        }

        byte statusByte = message[delimiterIndex + 1];

        if (statusByte != 0 && statusByte != 1) {
            throw new Exception("Invalid status byte");
        }

        boolean checked = statusByte == 1;

        System.out.println("Message parsed");
        System.out.println("Checkbox number: " + checkboxId);
        System.out.println("Checkbox status: " + checked);

        return new ParsedMessage(checkboxId, checked, new String(message));
    }

}
