package one.dastec.simplewebsocket;

public enum OpCode {
    CONT(0x0),
    TEXT(0x1),
    BINARY(0x2),
    CLOSE(0x8),
    PING(0x9),
    PONG(0xA);

    public int value;

    OpCode(int i) {
        this.value = i;
    }

    boolean equals(int i) {
        return this.value == i;
    }

    public static OpCode find(int i) {
        for (OpCode opcode : OpCode.values()) {
            if (opcode.value == i) {
                return opcode;
            }
        }
        return null;
    }
}
