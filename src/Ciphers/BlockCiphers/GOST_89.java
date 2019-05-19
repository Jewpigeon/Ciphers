package Ciphers.BlockCiphers;

import Ciphers.Basic.Cipher;
import Ciphers.Utils.AlgorithmUtil;
import Ciphers.Utils.BitUtil;


public class GOST_89 implements Cipher {
    public final static byte ECB = 0;
    public final static byte CNT = 1;
    public final static byte CFB = 2;
    private final static byte KEY_SIZE = 32;
    private final static byte IV_SIZE = 8;
    private static GOST_algorithm GOST;
    private byte MODE_SELECTED = 0;

    private GOST_89() {

    }

    private GOST_89(byte mode) {
        this.setMode(mode);
    }

    private GOST_89(byte[] key, byte mode) {
        this.setKey(key);
        this.setMode(mode);
    }

    private GOST_89(byte[] key, byte[] iv, byte mode) {
        this.setKey(key);
        this.setIV(iv);
        this.setMode(mode);
    }

    public static GOST_89 getInstance() {
        return new GOST_89();
    }

    public static GOST_89 getInstance(byte mode) {
        return new GOST_89(mode);
    }


    public static GOST_89 getInstance(byte mode, byte[] key) {
        return new GOST_89(key, mode);
    }

    public static GOST_89 getInstance(byte mode, byte[] key, byte[] iv) {
        return new GOST_89(key, iv, mode);
    }


    @Override
    public void setKey(byte[] key) {
        if (key.length != KEY_SIZE) throw new GOST_exception(GOST_exception.KEY_LEN);
        GOST = new GOST_algorithm(key);
    }

    public void setIV(byte[] IV) {
        if (IV.length != IV_SIZE) throw new GOST_exception(GOST_exception.IV_LEN);
        GOST.IV = IV;
    }

    public byte[] doMac(byte[] input) {
        return GOST.doMac(input);
    }

    public void setMode(byte mode) {
        this.MODE_SELECTED = (mode > 2 || mode < 0) ? 0 : mode;
    }

    @Override
    public byte[] encrypt(byte[] plain) {
        if (plain.length == 0) throw new GOST_exception(GOST_exception.DATA_NULL);
        switch (this.MODE_SELECTED) {
            case 2:
                return GOST.encryptInCFB(plain);
            case 1:
                return GOST.encryptInCNT(plain);
            default:
                return GOST.encryptInECB(plain);
        }

    }

    @Override
    public byte[] decrypt(byte[] ciph) {
        if (ciph.length == 0) throw new GOST_exception(GOST_exception.DATA_NULL);
        switch (this.MODE_SELECTED) {
            case 2:
                return GOST.decryptInCFB(ciph);
            case 1:
                return GOST.decryptInCNT(ciph);
            default:
                return GOST.decryptInECB(ciph);
        }
    }

    @Override
    public void reset() {
        this.MODE_SELECTED = 0;
        GOST = new GOST_algorithm();
    }


    private class GOST_algorithm {
        private final int C2 = 0x1010101;
        private final int C1 = 0x1010104;
        private final byte[][] S = new byte[][]{
                {0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF},
                {0xC, 0x4, 0x6, 0x2, 0xA, 0x5, 0xB, 0x9, 0xE, 0x8, 0xD, 0x7, 0x0, 0x3, 0xF, 0x1},
                {0x6, 0x8, 0x2, 0x3, 0x9, 0xA, 0x5, 0xC, 0x1, 0xE, 0x4, 0x7, 0xB, 0xD, 0x0, 0xF},
                {0xB, 0x3, 0x5, 0x8, 0x2, 0xF, 0xA, 0xD, 0xE, 0x1, 0x7, 0x4, 0xC, 0x9, 0x6, 0x0},
                {0xC, 0x8, 0x2, 0x1, 0xD, 0x4, 0xF, 0x6, 0x7, 0x0, 0xA, 0x5, 0x3, 0xE, 0x9, 0xB},
                {0x7, 0xF, 0x5, 0xA, 0x8, 0x1, 0x6, 0xD, 0x0, 0x9, 0x3, 0xE, 0xB, 0x4, 0x2, 0xC},
                {0x5, 0xD, 0xF, 0x6, 0x9, 0x2, 0xC, 0xA, 0xB, 0x7, 0x8, 0x1, 0x4, 0x3, 0xE, 0x0},
                {0x8, 0xE, 0x2, 0x5, 0x6, 0x9, 0x1, 0xC, 0xF, 0x4, 0xB, 0x0, 0xD, 0xA, 0x3, 0x7},
                {0x1, 0x7, 0xE, 0xD, 0x0, 0x5, 0x8, 0x3, 0x4, 0xF, 0xA, 0x6, 0x9, 0xC, 0xB, 0x2},
        };


        private byte[][] splittedKey = new byte[32][4];
        private byte[] IV = null;

        GOST_algorithm(byte[] K) {
            if (K == null || K.length != 32) throw new GOST_exception(GOST_exception.KEY_LEN);
            byte[][] splitted = BitUtil.Fission.splitTo4bytes(K);
            for (int i = 0; i < 8; i++) {
                System.arraycopy(splitted[i], 0, splittedKey[i], 0, 4);
                System.arraycopy(splitted[i], 0, splittedKey[i + 4], 0, 4);
                System.arraycopy(splitted[i], 0, splittedKey[i + 8], 0, 4);
                System.arraycopy(splitted[i], 0, splittedKey[i + 12], 0, 4);
                System.arraycopy(splitted[i], 0, splittedKey[i + 16], 0, 4);
                System.arraycopy(splitted[i], 0, splittedKey[32 - i - 1], 0, 4);
            }

        }

        GOST_algorithm() {
        }


        byte[] encryptInECB(byte[] input) {
            if (input.length % 8 != 0) throw new GOST_exception(GOST_exception.DATA_LEN);

            byte[] encrypted = new byte[input.length];

            for (int i = 0; i < input.length; i += 8) {
                byte[] chunck = new byte[8];
                System.arraycopy(input, i, chunck, 0, 8);
                byte[] A = new byte[4];
                byte[] B = new byte[4];
                System.arraycopy(chunck, 0, B, 0, 4);
                System.arraycopy(chunck, 4, A, 0, 4);

                for (int j = 0; j < 32; j++) {
                    byte[] temp = A;
                    A = BitUtil.Operation.Xor(B, f(A, splittedKey[j]));
                    B = temp;
                }
                byte[] e_chuck = new byte[8];
                System.arraycopy(A, 0, e_chuck, 0, 4);
                System.arraycopy(B, 0, e_chuck, 4, 4);
                System.arraycopy(e_chuck, 0, encrypted, i, 8);
            }
            return encrypted;
        }

        byte[] decryptInECB(byte[] input) {
            if (input.length % 8 != 0) throw new GOST_exception(GOST_exception.DATA_LEN);

            byte[] decrypted = new byte[input.length];
            byte[][] splittedKey_reversed = splittedKey.clone();
            AlgorithmUtil.reverseMatrix(splittedKey_reversed);

            for (int i = 0; i < input.length; i += 8) {

                byte[] chunck = new byte[8];
                System.arraycopy(input, i, chunck, 0, 8);

                byte[] A = new byte[4];
                byte[] B = new byte[4];

                System.arraycopy(chunck, 0, B, 0, 4);
                System.arraycopy(chunck, 4, A, 0, 4);

                for (int j = 0; j < 32; j++) {
                    byte[] temp = A;
                    A = BitUtil.Operation.Xor(B, f(A, splittedKey_reversed[j]));
                    B = temp;
                }

                byte[] d_chuck = new byte[8];

                System.arraycopy(A, 0, d_chuck, 0, 4);
                System.arraycopy(B, 0, d_chuck, 4, 4);
                System.arraycopy(d_chuck, 0, decrypted, i, 8);
            }

            return decrypted;
        }

        byte[] encryptInCNT(byte[] input) {
            if (this.IV == null) throw new GOST_exception(GOST_exception.IV_NULL);
            byte[] gamma = generateGamma(this.IV, input.length);
            return BitUtil.Operation.Xor(input, gamma);
        }

        byte[] decryptInCNT(byte[] input) {
            if (this.IV == null) throw new GOST_exception(GOST_exception.IV_NULL);
            byte[] gamma = generateGamma(this.IV, input.length);
            return BitUtil.Operation.Xor(input, gamma);
        }

        byte[] encryptInCFB(byte[] input) {
            if (this.IV == null) throw new GOST_exception(GOST_exception.IV_NULL);
            int len = BitUtil.Extend.extendToSize(input.length, 8);
            byte[] extended = new byte[len];

            System.arraycopy(input, 0, extended, 0, input.length);
            byte[] STATE = this.IV.clone();

            for (int i = 0; i < extended.length; i += 8) {
                STATE = encryptInECB(STATE);
                byte[] chunck = new byte[8];
                System.arraycopy(extended, i, chunck, 0, 8);
                chunck = BitUtil.Operation.Xor(chunck, STATE);
                System.arraycopy(chunck, 0, extended, i, 8);
                STATE = chunck;

            }

            byte[] encrypted = new byte[input.length];

            System.arraycopy(extended, 0, encrypted, 0, encrypted.length);
            return encrypted;
        }

        byte[] decryptInCFB(byte[] input) {
            if (this.IV == null) throw new GOST_exception(GOST_exception.IV_NULL);
            int len = BitUtil.Extend.extendToSize(input.length, 8);
            byte[] extended = new byte[len];
            System.arraycopy(input, 0, extended, 0, input.length);

            byte[] decrypted = new byte[input.length];

            byte[] STATE = this.IV.clone();
            for (int i = 0; i < input.length; i += 8) {
                STATE = encryptInECB(STATE);
                byte[] chunck = new byte[8];
                System.arraycopy(extended, i, chunck, 0, 8);
                System.arraycopy(BitUtil.Operation.Xor(chunck.clone(), STATE), 0, extended, i, 8);
                STATE = chunck;
            }
            System.arraycopy(extended, 0, decrypted, 0, decrypted.length);
            return decrypted;
        }

        byte[] doMac(byte[] input) {
            byte[] input_extended;
            if (input.length % 8 != 0) {
                input_extended = new byte[BitUtil.Extend.extendToSize(input.length, 8)];
                System.arraycopy(input, 0, input_extended, 0, input.length);
            } else {
                input_extended = input.clone();
            }
            byte[] MAC = new byte[8];
            System.arraycopy(input_extended, 0, MAC, 0, 8);
            for (int i = 8; i < input_extended.length - 8; i += 8) {
                MAC = encryptInECB16(MAC);
                byte[] chunck = new byte[8];
                System.arraycopy(input_extended, i, chunck, 0, 8);
                BitUtil.Operation.Xor(MAC, chunck);
            }
            return MAC;
        }

        private byte[] encryptInECB16(byte[] input) {
            if (input.length % 8 != 0) throw new GOST_exception(GOST_exception.DATA_LEN);
            byte[] encrypted = new byte[input.length];
            for (int i = 0; i < input.length; i += 8) {
                byte[] chunck = new byte[8];
                System.arraycopy(input, i, chunck, 0, 8);
                byte[] A = new byte[4];
                byte[] B = new byte[4];
                System.arraycopy(chunck, 0, B, 0, 4);
                System.arraycopy(chunck, 4, A, 0, 4);
                for (int j = 0; j < 16; j++) {
                    byte[] temp = A;
                    A = BitUtil.Operation.Xor(B, f(A, splittedKey[j]));
                    B = temp;
                }
                byte[] e_chuck = new byte[8];
                System.arraycopy(A, 0, e_chuck, 0, 4);
                System.arraycopy(B, 0, e_chuck, 4, 4);
                System.arraycopy(e_chuck, 0, encrypted, i, 8);
            }
            return encrypted;
        }

        private byte[] f(byte[] A, byte[] Ki) {
            byte[] A_Ki = BitUtil.ByteArrays.intToByteArray((BitUtil.ByteArrays.byteArrayToInt(A) + BitUtil.ByteArrays.byteArrayToInt(Ki)));
            A_Ki = BitUtil.Fission.splitBy4bits(A_Ki);
            for (int i = 0; i < A_Ki.length; i++) {
                A_Ki[i] = S[i][AlgorithmUtil.binarySearch(S[0], (A_Ki[i]), 0, 16)];
            }
            A_Ki = BitUtil.Fission.concatBy4bit(A_Ki);
            A_Ki = BitUtil.ByteArrays.intToByteArray(BitUtil.BitRotation.rotateL(BitUtil.ByteArrays.byteArrayToInt(A_Ki), 11));
            return A_Ki;
        }

        private byte[] generateGamma(byte[] iv, int len) {
            byte[] iv_clone = encryptInECB(iv);
            byte[] gamma_extended = new byte[BitUtil.Extend.extendToSize(len, 8)];
            byte[] N1 = new byte[4];
            byte[] N2 = new byte[4];
            System.arraycopy(iv_clone, 0, N1, 0, 4);
            byte[] gamma_1 = gamma_round(N1, N2);
            System.arraycopy(gamma_1, 0, gamma_extended, 0, 8);

            for (int i = 0; i < len; i += 8) {
                System.arraycopy(gamma_extended, i, N1, 0, 4);
                System.arraycopy(gamma_extended, i + 4, N2, 0, 4);
                gamma_1 = gamma_round(N1, N2);
                System.arraycopy(gamma_1, 0, gamma_extended, i, 8);
            }

            byte[] gamma = new byte[len];
            System.arraycopy(gamma_extended, 0, gamma, 0, gamma.length);
            return gamma;
        }

        private byte[] gamma_round(byte[] N1, byte[] N2) {
            byte[] gamma = new byte[8];
            N1 = BitUtil.ByteArrays.intToByteArray(BitUtil.ByteArrays.byteArrayToInt(N1) + C2);
            N2 = BitUtil.ByteArrays.intToByteArray(BitUtil.ByteArrays.byteArrayToInt(N2) + C1);
            System.arraycopy(N1, 0, gamma, 0, 4);
            System.arraycopy(N2, 0, gamma, 4, 4);
            gamma = encryptInECB(gamma);
            return gamma;

        }
    }

    private class GOST_exception extends RuntimeException {
        final static String IV_NULL = "Initialization Vector is not set! Set it with GOST_89.setIV(byte[] IV)";
        final static String IV_LEN = "IV length must be 64 bits (8 bytes)!";
        final static String DATA_LEN = "Input length must be multiple of 8 (64 bits)";
        final static String KEY_LEN = "Key length must be 256 bits (32 bytes)!";
        final static String DATA_NULL = "Data length must be >0!";


        GOST_exception() {
            super();
        }


        GOST_exception(String message) {
            super(message);
        }


    }


}
