package Ciphers.BlockCiphers;

import Ciphers.Utils.AlgorithmUtil;
import Ciphers.Utils.BitUtil;

import java.util.Arrays;

import static Ciphers.Utils.BitUtil.Operation.XOR;

class REDOCII extends BlockCipher {
    private REDOCII_algorithm REDOC_A;
    private final int[] KEY_SIZES = new int[]{8, 2240};

    REDOCII() {
        REDOC_A = new REDOCII_algorithm();
        super.algorithm = REDOC_A;
        super.KeySize = 2240;
        super.IV_Size = 10;
        super.PossibleKeySizes = KEY_SIZES;
    }

    @Override
    public void setKey(byte[] key) {
        if (key == null || (key.length < KEY_SIZES[0] || key.length > KEY_SIZES[1]))
            throw new BlockCipherException(BlockCipherException.KEY_LEN_MANY, "from 80 up to 17 920 (%8 = 0)", "from 8 up to 2240");
        byte[] IV_save = REDOC_A == null ? null : REDOC_A.IV;
        REDOC_A = new REDOCII_algorithm(key);
        REDOC_A.IV = IV_save;
        super.algorithm = REDOC_A;
    }

    private class REDOCII_algorithm extends BlockCipher.BlockCipherAlgorithm {
        private byte[][] KEY_TABLE = new byte[128][10];
        private byte[][] MASK_TABLE = new byte[4][10];

        REDOCII_algorithm() {
            super.blocksize = 10;

        }

        REDOCII_algorithm(byte[] key) {
            super.blocksize = 10;
            if(this.IV!=null) key = BitUtil.Operation.XOR(key,IV);
            generateKeyTable(key);
            generateMaskTable();

        }

        private void generateKeyTable(byte[] key) {
            key = new byte[]{27, 115, 21, 1, 12, 41, 2, 92, 17, 81};
            KEY_TABLE[0] = KEY_STEP(key);
            for (int k = 1; k < KEY_TABLE.length; k++) {
                KEY_TABLE[k] = KEY_STEP(KEY_TABLE[k - 1]);
            }
        }

        private void generateMaskTable() {
            int start = 0;
            for (int MASK_CTR = 0; MASK_CTR < 4; MASK_CTR++) {
                int KEY_BYTE = 0;
                for (int i = 0; i < 10; i++) {
                    int sumBytes = 0;
                    for (int j = start; j < start + 32; j++) {
                        sumBytes += KEY_TABLE[j][KEY_BYTE];
                    }
                    sumBytes &= 0x7f;
                    MASK_TABLE[MASK_CTR][i] = (byte) sumBytes;
                    KEY_BYTE++;
                }
                start += 32;
            }

        }

        @Override
        byte[] encryptInECB(byte[] input) {
            byte[] encrypted = input.clone();
            for (int k = 0; k < encrypted.length; k+=10) {
                byte[] BLOCK = new byte[10];
                System.arraycopy(encrypted,k,BLOCK,0,10);
                for (int R = 0; R < 10; R++) {
                    int R_1 = (R == 9 ? R : (R + 1));
                    int sum = 0;
                    for (byte add : BLOCK) {
                        sum += add;
                    }
                    sum &= 0x7f;
                    int PERMUTATION_INDEX = (MASK_TABLE[0][R] ^ sum) & 0x7f;
                    BLOCK = PERMUTATE_BLOCK(BLOCK, PERMUTATION_INDEX);
                    byte KEY = (byte) ((MASK_TABLE[1][R] ^ BLOCK[R]) & 0x7f);
                    BLOCK = KEY_ADDITION(BLOCK, KEY, R);
                    KEY = (byte) ((MASK_TABLE[1][R] ^ BLOCK[R_1]) & 0x7f);
                    BLOCK = KEY_ADDITION(BLOCK, KEY, R_1);

                    BLOCK = ENCLAVE_PROCESS(BLOCK, MASK_TABLE[2][R] & 0x1f);
                    byte SUB_INDEX = (byte) ((MASK_TABLE[3][R] ^ BLOCK[R]) & 0xf);
                    BLOCK = SUBSTITUDE_BLOCK(BLOCK, SUB_INDEX, R);
                    SUB_INDEX = (byte) ((MASK_TABLE[3][R] ^ BLOCK[R_1]) & 0xf);
                    BLOCK = SUBSTITUDE_BLOCK(BLOCK, SUB_INDEX, R_1);
                }
                System.arraycopy(BLOCK,0,encrypted,k,10);
            }
            return encrypted;

        }

        @Override
        byte[] decryptInECB(byte[] input) {
            byte[] decrypted = input.clone();
            for (int k = 0; k < decrypted.length; k+=10) {
                byte[] BLOCK = new byte[10];
                System.arraycopy(decrypted, k, BLOCK, 0, 10);
                for (int R = 9; R >= 0; R--) {
                    int R_1 = R == 9 ? R : R + 1;
                    byte SUB_INDEX = (byte) ((MASK_TABLE[3][R] ^ BLOCK[R_1]) & 0xf);
                    BLOCK = INVERSE_SUBSTITUDE_BLOCK(BLOCK, SUB_INDEX, R_1);
                    SUB_INDEX = (byte) ((MASK_TABLE[3][R] ^ BLOCK[R]) & 0xf);
                    BLOCK = INVERSE_SUBSTITUDE_BLOCK(BLOCK, SUB_INDEX, R);

                    BLOCK = INVERSE_ENCLAVE_PROCESS(BLOCK, MASK_TABLE[2][R] & 0x1f);

                    byte KEY = (byte) ((MASK_TABLE[1][R] ^ BLOCK[R_1]) & 0x7f);
                    BLOCK = KEY_ADDITION(BLOCK, KEY, R_1);
                    KEY = (byte) ((MASK_TABLE[1][R] ^ BLOCK[R]) & 0x7f);
                    BLOCK = KEY_ADDITION(BLOCK, KEY, R);
                    int sum = 0;
                    for (byte add : BLOCK) {
                        sum += add;
                    }
                    sum &= 0x7f;
                    int PERMUTATION_INDEX = (MASK_TABLE[0][R] ^ sum) & 0x7f;
                    BLOCK = INVERSE_PERMUTATE_BLOCK(BLOCK, PERMUTATION_INDEX);
                }
                System.arraycopy(BLOCK,0,decrypted,k,10);
            }
            return decrypted;
        }

        private byte[] ENCLAVE_PROCESS(byte[] BLOCK, int ENCLAVE_TABLE_NUM) {
            byte[] L = new byte[5], R = new byte[5];
            System.arraycopy(BLOCK, 0, L, 0, 5);
            System.arraycopy(BLOCK, 5, R, 0, 5);
            int ENCLAVE_START = ENCLAVE_TABLE_NUM * 4;
            R = AUTOCLAVE(R, ENCLAVE_TABLE[ENCLAVE_START]);
            R = AUTOCLAVE(R, ENCLAVE_TABLE[ENCLAVE_START + 1]);
            byte[] LxR = XOR(R, L);
            LxR = AUTOCLAVE(LxR, ENCLAVE_TABLE[ENCLAVE_START + 2]);
            LxR = AUTOCLAVE(LxR, ENCLAVE_TABLE[ENCLAVE_START + 3]);
            byte[] RxP = XOR(LxR, R);
            byte[] E = new byte[10];
            System.arraycopy(LxR, 0, E, 0, 5);
            System.arraycopy(RxP, 0, E, 5, 5);
            return E;
        }

        private byte[] AUTOCLAVE(byte[] BLOCK, byte[][] ENCLAVE_CURRENT) {
            int j = 0;
            for (int i = 0; i < 5; i++) {
                byte[] chang_a = new byte[]{(byte) (ENCLAVE_CURRENT[0][j] - 1), (byte) (ENCLAVE_CURRENT[1][j] - 1), (byte) (ENCLAVE_CURRENT[2][j] - 1)};
                byte chang_cur = chang_a[0];
                BLOCK[chang_cur] = (byte) ((BLOCK[chang_a[0]] + BLOCK[chang_a[1]] + BLOCK[chang_a[2]]) & 0x7f);
                j++;
            }
            return BLOCK;

        }

        private byte[] KEY_STEP(byte[] KEY_O) {
            byte[] KEY = KEY_O.clone();
            int X = 0;
            int Y = 0;
            for (int i = 0; i < 5; i++)  X += KEY[i];
            X &= 0x7f;
            for (int i = 5; i < 10; i++) Y += KEY[i];
            Y &= 0xf;

            KEY = SUBSTITUDE_BLOCK(KEY, Y);
            KEY = PERMUTATE_BLOCK(KEY, X);
            int ENCLAVE = 0;
            for (int i = 2; i < 7; i++) ENCLAVE += KEY[i];
            KEY = ENCLAVE_PROCESS(KEY, ENCLAVE & 0x1f);
            return KEY;
        }

        private byte[] KEY_ADDITION(byte[] BLOCK, byte KEY, int R) {
            byte save = BLOCK[R];
            BLOCK = XOR(BLOCK, KEY_TABLE[KEY]);
            BLOCK[R] = save;
            return BLOCK;
        }

        private byte[] SUBSTITUDE_BLOCK(byte[] BLOCK, int INDEX, int R) {
            byte save = BLOCK[R];
            for (int i = 0; i < BLOCK.length; i++) BLOCK[i] = SUBSTITION_TABLE[INDEX][BLOCK[i]];
            BLOCK[R] = save;
            return BLOCK;
        }

        private byte[] PERMUTATE_BLOCK(byte[] BLOCK, int INDEX) {
            byte[] BLOCK_NEW = new byte[10];
            for (int i = 0; i < BLOCK.length; i++) BLOCK_NEW[PERMUTATION_TABLE[INDEX][i] - 1] = BLOCK[i];
            return BLOCK_NEW;
        }

        private byte[] SUBSTITUDE_BLOCK(byte[] BLOCK, int INDEX) {
            for (int i = 0; i < BLOCK.length; i++) BLOCK[i] = SUBSTITION_TABLE[INDEX][BLOCK[i]];
            return BLOCK;
        }

        private byte[] INVERSE_SUBSTITUDE_BLOCK(byte[] BLOCK, int INDEX, int R) {
            byte save = BLOCK[R];
            for (int i = 0; i < BLOCK.length; i++)  BLOCK[i] = (byte) (AlgorithmUtil.indexOfElement(SUBSTITION_TABLE[INDEX],BLOCK[i]) & 0x7f);
            BLOCK[R] = save;
            return BLOCK;
        }

        private byte[] INVERSE_PERMUTATE_BLOCK(byte[] BLOCK, int INDEX){
            byte[] BLOCK_NEW = new byte[10];
            for (int i = 0; i < BLOCK.length; i++) BLOCK_NEW[i] = BLOCK[PERMUTATION_TABLE[INDEX][i]-1];
            return BLOCK_NEW;
        }

        private byte[] INVERSE_ENCLAVE_PROCESS(byte[] BLOCK, int ENCLAVE_TABLE_NUM){
            byte[] L = new byte[5], R = new byte[5];
            System.arraycopy(BLOCK, 0, L, 0, 5);
            System.arraycopy(BLOCK, 5, R, 0, 5);
            int ENCLAVE_START = ENCLAVE_TABLE_NUM * 4;
            byte[] LxR = BitUtil.Operation.XOR(L,R);
            L = INVERSE_AUTOCLAVE(L, ENCLAVE_TABLE[ENCLAVE_START+3]);
            L = INVERSE_AUTOCLAVE(L, ENCLAVE_TABLE[ENCLAVE_START+2]);
            L = BitUtil.Operation.XOR(L,LxR);
            LxR = INVERSE_AUTOCLAVE(LxR,ENCLAVE_TABLE[ENCLAVE_START+1]);
            LxR = INVERSE_AUTOCLAVE(LxR,ENCLAVE_TABLE[ENCLAVE_START]);
            byte[] D = new byte[10];
            System.arraycopy(L, 0, D, 0, 5);
            System.arraycopy(LxR, 0, D, 5, 5);
            return D;
        }

        private byte[] INVERSE_AUTOCLAVE(byte [] BLOCK, byte[][] ENCLAVE_CURRENT){
            int j = 4;
            for (int i = 0; i<5; i++) {
                byte[] chang_a = new byte[]{(byte) (ENCLAVE_CURRENT[0][j] - 1), (byte) (ENCLAVE_CURRENT[1][j] - 1), (byte) (ENCLAVE_CURRENT[2][j] - 1)};
                byte chang_cur = chang_a[0];
                BLOCK[chang_cur] = (byte) ((BLOCK[chang_a[0]] - BLOCK[chang_a[1]] - BLOCK[chang_a[2]]) & 0x7f);
                j--;
            }
            return BLOCK;
        }

        private final byte[][] PERMUTATION_TABLE = new byte[][]{
                {1, 6, 7, 9, 10, 2, 5, 8, 3, 4},
                {10, 4, 8, 3, 1, 7, 2, 9, 5, 6},
                {1, 6, 4, 9, 8, 5, 10, 2, 3, 7},
                {9, 8, 3, 4, 5, 10, 6, 1, 7, 2},
                {9, 4, 6, 3, 8, 1, 10, 2, 5, 7},
                {5, 2, 4, 9, 1, 6, 10, 7, 8, 3},
                {2, 8, 6, 1, 5, 9, 3, 4, 10, 7},
                {7, 8, 10, 2, 5, 4, 3, 1, 9, 6},
                {1, 2, 10, 3, 8, 7, 4, 6, 9, 5},
                {10, 8, 2, 3, 5, 9, 7, 1, 6, 4},
                {7, 3, 8, 5, 4, 1, 2, 9, 10, 6},
                {6, 5, 7, 2, 10, 4, 3, 9, 1, 8},
                {8, 5, 2, 7, 6, 3, 9, 1, 4, 10},
                {4, 1, 6, 7, 5, 10, 2, 3, 8, 9},
                {10, 2, 7, 1, 5, 4, 8, 9, 6, 3},
                {3, 5, 7, 9, 8, 1, 2, 10, 4, 6},
                {6, 8, 9, 5, 3, 7, 10, 4, 1, 2},
                {2, 6, 1, 4, 7, 5, 3, 9, 10, 8},
                {2, 5, 4, 9, 10, 3, 8, 6, 7, 1},
                {3, 10, 5, 8, 6, 7, 4, 2, 1, 9},
                {9, 10, 5, 6, 3, 7, 2, 1, 4, 8},
                {3, 5, 4, 7, 6, 1, 2, 8, 10, 9},
                {6, 5, 2, 7, 1, 9, 10, 8, 3, 4},
                {4, 1, 3, 6, 7, 8, 9, 10, 5, 2},
                {7, 3, 5, 1, 6, 4, 9, 10, 8, 2},
                {7, 5, 4, 9, 1, 3, 6, 8, 10, 2},
                {7, 1, 5, 10, 9, 2, 4, 6, 8, 3},
                {5, 1, 3, 10, 9, 7, 8, 2, 4, 6},
                {6, 3, 4, 9, 1, 8, 2, 7, 5, 10},
                {1, 2, 4, 9, 7, 3, 10, 8, 5, 6},
                {8, 2, 9, 4, 3, 7, 1, 6, 10, 5},
                {3, 4, 9, 10, 8, 5, 1, 6, 2, 7},
                {9, 10, 2, 1, 6, 8, 4, 5, 7, 3},
                {5, 1, 7, 6, 4, 8, 9, 10, 2, 3},
                {10, 3, 6, 9, 4, 2, 5, 7, 8, 1},
                {9, 1, 6, 7, 4, 8, 3, 5, 2, 10},
                {8, 9, 4, 7, 10, 2, 6, 1, 5, 3},
                {4, 6, 7, 5, 2, 1, 3, 9, 10, 8},
                {3, 9, 7, 4, 10, 2, 1, 6, 8, 5},
                {2, 4, 5, 6, 7, 10, 1, 8, 9, 3},
                {5, 1, 2, 4, 8, 10, 6, 9, 7, 3},
                {10, 1, 5, 8, 2, 7, 4, 6, 3, 9},
                {5, 3, 4, 9, 2, 10, 7, 6, 1, 8},
                {6, 5, 10, 4, 1, 2, 9, 8, 3, 7},
                {9, 1, 7, 6, 4, 5, 10, 3, 8, 2},
                {9, 7, 2, 10, 5, 8, 4, 6, 3, 1},
                {4, 10, 5, 1, 2, 8, 7, 9, 6, 3},
                {7, 3, 2, 6, 10, 5, 8, 4, 9, 1},
                {3, 1, 6, 9, 7, 10, 5, 2, 8, 4},
                {6, 4, 2, 1, 7, 3, 9, 10, 5, 8},
                {2, 8, 3, 9, 7, 1, 6, 4, 5, 10},
                {3, 10, 4, 7, 1, 5, 6, 2, 8, 9},
                {5, 1, 3, 6, 10, 4, 7, 9, 2, 8},
                {10, 5, 2, 4, 9, 1, 6, 7, 8, 3},
                {5, 9, 2, 8, 6, 3, 4, 10, 1, 7},
                {6, 1, 5, 10, 8, 4, 2, 3, 9, 7},
                {2, 9, 3, 1, 6, 10, 8, 4, 5, 7},
                {2, 5, 3, 6, 10, 9, 1, 8, 7, 4},
                {9, 7, 1, 6, 10, 2, 3, 5, 4, 8},
                {2, 10, 4, 5, 1, 9, 6, 7, 8, 3},
                {7, 2, 6, 4, 1, 9, 10, 3, 8, 5},
                {3, 2, 4, 5, 8, 10, 7, 6, 9, 1},
                {1, 7, 8, 3, 9, 10, 6, 5, 4, 2},
                {6, 9, 4, 3, 8, 10, 7, 2, 5, 1},
                {7, 1, 6, 2, 4, 5, 8, 10, 3, 9},
                {7, 2, 5, 9, 1, 6, 10, 3, 4, 8},
                {10, 5, 4, 3, 8, 9, 1, 6, 7, 2},
                {9, 2, 3, 6, 8, 7, 5, 4, 1, 10},
                {9, 4, 6, 10, 8, 7, 5, 1, 3, 2},
                {1, 2, 7, 3, 4, 5, 10, 8, 6, 9},
                {2, 6, 8, 5, 10, 1, 3, 4, 9, 7},
                {9, 4, 3, 1, 5, 6, 10, 8, 2, 7},
                {1, 8, 7, 10, 3, 9, 6, 4, 5, 2},
                {3, 10, 7, 9, 4, 6, 5, 1, 8, 2},
                {8, 5, 9, 6, 7, 10, 3, 4, 2, 1},
                {8, 4, 10, 1, 6, 5, 9, 2, 7, 3},
                {8, 6, 9, 10, 5, 7, 1, 4, 2, 3},
                {10, 4, 5, 7, 8, 6, 9, 3, 2, 1},
                {1, 5, 8, 10, 3, 9, 6, 7, 4, 2},
                {4, 9, 7, 5, 8, 2, 3, 1, 10, 6},
                {1, 8, 9, 7, 3, 2, 5, 6, 10, 4},
                {1, 2, 9, 8, 5, 4, 6, 7, 3, 10},
                {7, 9, 6, 2, 1, 8, 4, 10, 5, 3},
                {10, 4, 8, 3, 5, 2, 6, 9, 1, 7},
                {8, 5, 7, 3, 2, 9, 1, 4, 6, 10},
                {9, 10, 3, 1, 4, 7, 6, 5, 8, 2},
                {9, 7, 2, 6, 5, 8, 3, 10, 1, 4},
                {5, 3, 8, 1, 9, 7, 10, 2, 4, 6},
                {6, 9, 1, 8, 2, 3, 7, 10, 5, 4},
                {4, 7, 9, 5, 2, 8, 10, 3, 6, 1},
                {8, 5, 1, 4, 6, 9, 2, 10, 3, 7},
                {10, 2, 4, 8, 3, 7, 9, 5, 6, 1},
                {4, 2, 3, 9, 5, 7, 8, 10, 1, 6},
                {9, 4, 10, 5, 3, 1, 2, 8, 7, 6},
                {3, 2, 6, 5, 4, 9, 8, 10, 7, 1},
                {6, 4, 10, 3, 7, 9, 5, 1, 2, 8},
                {6, 8, 2, 9, 3, 10, 7, 5, 4, 1},
                {10, 4, 8, 7, 9, 5, 3, 2, 1, 6},
                {2, 1, 5, 7, 10, 9, 3, 8, 6, 4},
                {3, 6, 10, 5, 8, 2, 9, 7, 4, 1},
                {3, 8, 2, 6, 7, 5, 4, 9, 1, 10},
                {8, 1, 9, 3, 6, 7, 4, 10, 2, 5},
                {4, 10, 2, 8, 6, 3, 9, 5, 7, 1},
                {4, 8, 2, 3, 7, 1, 10, 5, 6, 9},
                {8, 5, 1, 7, 4, 6, 2, 3, 10, 9},
                {7, 10, 5, 1, 6, 8, 4, 3, 9, 2},
                {2, 5, 3, 8, 10, 9, 6, 4, 1, 7},
                {2, 9, 6, 1, 7, 8, 5, 4, 3, 10},
                {4, 2, 3, 10, 9, 5, 7, 8, 6, 1},
                {5, 2, 10, 8, 4, 1, 3, 7, 6, 9},
                {6, 5, 8, 3, 7, 4, 9, 10, 2, 1},
                {10, 7, 1, 3, 6, 4, 9, 2, 5, 8},
                {9, 7, 8, 4, 6, 1, 2, 5, 3, 10},
                {3, 4, 8, 7, 2, 5, 10, 9, 1, 6},
                {5, 8, 7, 1, 9, 2, 6, 10, 4, 3},
                {5, 4, 3, 1, 2, 8, 10, 7, 9, 6},
                {9, 7, 1, 3, 5, 6, 8, 2, 4, 10},
                {9, 4, 10, 6, 1, 2, 7, 5, 3, 8},
                {1, 6, 5, 10, 9, 8, 2, 7, 4, 3},
                {10, 3, 8, 2, 5, 6, 7, 1, 9, 4},
                {6, 10, 2, 5, 8, 3, 4, 9, 7, 1},
                {6, 1, 8, 10, 5, 4, 2, 7, 9, 3},
                {9, 10, 8, 2, 5, 1, 3, 7, 4, 6},
                {1, 3, 7, 6, 2, 9, 5, 4, 10, 8},
                {7, 6, 1, 5, 3, 9, 8, 2, 10, 4},
                {4, 7, 10, 6, 1, 8, 2, 5, 3, 9},
                {9, 8, 3, 7, 1, 10, 5, 6, 2, 4},
                {7, 8, 5, 10, 9, 3, 4, 2, 1, 6}
        };


        private final byte[][] SUBSTITION_TABLE = new byte[][]{
                {90, 46, 66, 21, 50, 57, 84, 67, 80, 91, 44, 124, 94, 126, 25, 125, 8, 37, 82, 28, 33, 14, 30, 115, 0, 71, 83, 68, 18, 123, 92, 34, 74, 97, 4, 53, 76, 27, 5, 35, 70, 43, 127, 79, 81, 16, 42, 32, 51, 106, 104, 120, 87, 48, 22, 45, 118, 54, 75, 10, 121, 85, 119, 100, 61, 116, 110, 86, 89, 9, 12, 13, 108, 69, 93, 55, 1, 52, 95, 20, 107, 64, 29, 23, 47, 40, 26, 58, 114, 65, 17, 36, 59, 2, 72, 39, 111, 15, 38, 60, 103, 19, 102, 77, 99, 109, 98, 56, 88, 96, 11, 6, 73, 101, 117, 62, 112, 41, 105, 63, 113, 7, 78, 49, 3, 31, 24, 122},
                {47, 89, 87, 20, 15, 84, 0, 65, 83, 4, 49, 70, 66, 110, 37, 73, 127, 68, 38, 64, 103, 124, 96, 126, 85, 112, 111, 86, 74, 53, 52, 82, 123, 93, 79, 9, 80, 33, 10, 27, 35, 88, 48, 19, 61, 45, 60, 40, 125, 36, 92, 57, 95, 99, 3, 67, 105, 75, 5, 71, 116, 77, 100, 98, 101, 115, 44, 30, 56, 81, 51, 32, 117, 26, 76, 120, 54, 1, 104, 121, 69, 72, 24, 41, 8, 63, 12, 31, 102, 113, 109, 25, 13, 91, 2, 21, 17, 55, 22, 122, 16, 18, 106, 39, 94, 6, 97, 7, 29, 46, 23, 108, 78, 58, 114, 90, 107, 59, 28, 34, 50, 11, 43, 119, 42, 118, 14, 62},
                {19, 44, 95, 25, 87, 38, 36, 125, 111, 71, 97, 43, 35, 84, 94, 107, 58, 24, 98, 4, 89, 53, 63, 52, 96, 33, 100, 116, 108, 28, 10, 93, 102, 65, 51, 83, 91, 55, 99, 110, 117, 56, 127, 86, 42, 120, 77, 18, 16, 67, 39, 14, 122, 72, 15, 73, 62, 13, 113, 69, 31, 79, 57, 92, 78, 76, 103, 121, 109, 106, 26, 37, 54, 40, 9, 32, 80, 8, 48, 41, 115, 23, 1, 27, 74, 60, 46, 112, 64, 50, 49, 17, 114, 0, 81, 90, 105, 47, 126, 66, 119, 61, 2, 5, 59, 101, 6, 70, 12, 123, 85, 29, 20, 7, 45, 68, 82, 30, 3, 34, 124, 21, 88, 104, 11, 118, 75, 22},
                {90, 26, 75, 106, 4, 54, 46, 41, 115, 107, 124, 36, 113, 126, 123, 35, 67, 58, 109, 52, 101, 10, 72, 114, 14, 42, 7, 98, 84, 43, 62, 71, 39, 31, 2, 5, 80, 91, 37, 105, 20, 34, 49, 61, 70, 44, 116, 87, 112, 111, 15, 29, 120, 11, 85, 97, 78, 73, 3, 74, 1, 65, 122, 102, 25, 79, 77, 9, 32, 63, 28, 103, 94, 68, 81, 47, 59, 92, 56, 121, 99, 8, 23, 51, 64, 57, 89, 40, 27, 88, 127, 6, 50, 12, 38, 86, 69, 18, 119, 48, 21, 16, 22, 33, 24, 66, 125, 0, 104, 82, 110, 76, 45, 13, 118, 117, 19, 83, 17, 100, 96, 95, 60, 30, 53, 93, 55, 108},
                {25, 51, 103, 116, 37, 88, 122, 7, 15, 44, 13, 111, 30, 120, 106, 114, 27, 4, 89, 78, 23, 34, 107, 126, 123, 12, 112, 117, 96, 50, 80, 102, 119, 81, 82, 101, 8, 72, 67, 108, 47, 21, 6, 66, 109, 35, 48, 94, 26, 97, 65, 85, 77, 99, 52, 57, 113, 39, 83, 74, 16, 49, 121, 93, 61, 68, 62, 84, 29, 0, 118, 17, 98, 110, 54, 18, 115, 60, 46, 73, 28, 86, 53, 127, 100, 91, 63, 71, 45, 31, 14, 43, 36, 33, 90, 87, 32, 64, 38, 5, 55, 56, 124, 3, 70, 58, 19, 79, 59, 40, 42, 104, 10, 75, 125, 2, 95, 1, 69, 22, 92, 24, 20, 9, 41, 76, 105, 11},
                {123, 85, 71, 13, 117, 92, 47, 107, 29, 98, 104, 108, 58, 48, 60, 89, 21, 14, 112, 121, 118, 67, 79, 22, 36, 24, 31, 41, 97, 10, 65, 52, 32, 81, 37, 9, 122, 6, 73, 57, 116, 35, 111, 18, 0, 1, 94, 26, 66, 115, 91, 55, 83, 82, 2, 11, 27, 87, 77, 88, 16, 86, 5, 56, 113, 40, 7, 72, 38, 46, 100, 84, 74, 68, 114, 19, 80, 54, 17, 93, 99, 95, 44, 78, 119, 15, 125, 4, 110, 12, 45, 28, 43, 3, 90, 124, 101, 127, 51, 69, 120, 23, 126, 61, 96, 75, 102, 25, 49, 109, 30, 33, 103, 50, 63, 76, 53, 64, 70, 8, 105, 62, 20, 34, 42, 59, 106, 39},
                {55, 122, 123, 7, 68, 94, 88, 79, 110, 117, 47, 67, 83, 69, 9, 93, 13, 120, 62, 37, 90, 10, 115, 98, 52, 85, 77, 50, 109, 104, 76, 95, 64, 44, 111, 2, 59, 33, 80, 78, 40, 45, 17, 31, 87, 22, 73, 15, 82, 61, 75, 118, 89, 25, 42, 74, 43, 48, 36, 4, 0, 51, 53, 97, 126, 92, 16, 119, 32, 8, 108, 24, 106, 49, 124, 21, 107, 121, 6, 101, 63, 60, 27, 71, 38, 39, 127, 102, 99, 58, 100, 11, 86, 34, 56, 113, 5, 103, 41, 19, 84, 14, 70, 112, 28, 81, 72, 26, 96, 65, 18, 46, 54, 23, 57, 35, 29, 66, 114, 105, 12, 1, 91, 125, 20, 30, 3, 116},
                {11, 54, 82, 62, 101, 70, 108, 119, 17, 121, 124, 87, 25, 74, 7, 126, 29, 111, 79, 58, 18, 77, 116, 102, 14, 107, 8, 4, 12, 92, 66, 27, 84, 105, 113, 45, 90, 96, 33, 52, 65, 114, 83, 98, 34, 115, 50, 75, 61, 20, 109, 118, 97, 43, 49, 36, 122, 15, 81, 89, 123, 10, 9, 63, 100, 93, 16, 30, 32, 31, 22, 56, 44, 110, 91, 120, 69, 13, 1, 103, 6, 40, 21, 86, 117, 112, 78, 72, 24, 42, 28, 41, 39, 53, 104, 37, 60, 57, 68, 35, 67, 0, 19, 71, 73, 95, 80, 106, 46, 64, 99, 85, 5, 38, 88, 26, 55, 23, 48, 51, 59, 94, 3, 127, 47, 2, 125, 76},
                {42, 32, 35, 20, 13, 38, 14, 2, 78, 81, 118, 59, 46, 107, 31, 8, 115, 61, 82, 49, 102, 87, 125, 39, 117, 53, 116, 80, 30, 25, 112, 57, 50, 40, 120, 105, 21, 47, 101, 94, 5, 76, 54, 34, 92, 66, 15, 111, 127, 100, 77, 7, 19, 70, 68, 104, 48, 83, 71, 119, 98, 72, 89, 85, 41, 109, 62, 74, 22, 0, 121, 29, 114, 3, 91, 36, 4, 51, 52, 86, 1, 67, 43, 103, 63, 113, 56, 55, 18, 96, 75, 6, 11, 110, 45, 9, 16, 124, 69, 95, 79, 126, 97, 88, 60, 26, 33, 123, 28, 64, 23, 12, 108, 99, 106, 65, 24, 10, 17, 90, 93, 73, 122, 44, 37, 84, 27, 58},
                {18, 124, 109, 17, 37, 119, 100, 89, 83, 96, 28, 52, 102, 126, 91, 104, 35, 26, 105, 62, 85, 15, 125, 66, 107, 21, 110, 50, 48, 67, 111, 5, 63, 90, 59, 106, 80, 9, 57, 116, 81, 99, 19, 84, 77, 49, 123, 33, 1, 60, 98, 65, 38, 41, 95, 58, 118, 97, 88, 72, 46, 23, 120, 2, 29, 93, 44, 34, 40, 45, 16, 30, 112, 117, 22, 115, 24, 122, 114, 70, 36, 73, 47, 121, 27, 71, 43, 87, 3, 79, 75, 101, 74, 108, 0, 42, 82, 39, 53, 10, 20, 54, 113, 4, 64, 7, 11, 92, 32, 13, 55, 103, 51, 94, 31, 56, 86, 61, 6, 78, 127, 76, 14, 25, 8, 12, 68, 69},
                {66, 13, 31, 7, 97, 91, 49, 56, 51, 9, 15, 93, 87, 45, 33, 46, 98, 70, 95, 115, 20, 11, 82, 6, 85, 123, 19, 79, 119, 24, 74, 3, 16, 116, 103, 0, 23, 122, 40, 62, 105, 104, 32, 37, 17, 113, 99, 77, 55, 18, 38, 35, 68, 86, 44, 72, 73, 89, 48, 47, 118, 90, 1, 41, 58, 107, 8, 112, 14, 102, 100, 42, 5, 53, 88, 64, 109, 108, 101, 2, 71, 124, 27, 92, 43, 121, 57, 84, 75, 78, 26, 111, 67, 126, 110, 117, 34, 50, 39, 10, 12, 127, 65, 83, 28, 30, 61, 81, 60, 54, 21, 25, 94, 125, 59, 36, 69, 96, 29, 22, 76, 80, 4, 106, 120, 52, 114, 63}, {24, 0, 69, 53, 45, 26, 121, 87, 82, 22, 97, 127, 78, 59, 68, 116, 106, 83, 107, 110, 35, 29, 104, 33, 118, 76, 18, 114, 54, 63, 74, 41, 71, 123, 112, 32, 2, 125, 100, 34, 94, 5, 19, 46, 4, 115, 13, 117, 124, 62, 56, 92, 102, 55, 108, 51, 72, 50, 37, 8, 36, 70, 79, 48, 89, 9, 15, 111, 122, 90, 91, 58, 86, 75, 93, 120, 3, 40, 7, 17, 67, 119, 42, 27, 77, 43, 64, 25, 30, 109, 103, 95, 126, 16, 88, 113, 52, 11, 98, 49, 99, 21, 57, 44, 12, 84, 28, 105, 10, 85, 73, 6, 14, 80, 96, 60, 38, 66, 20, 1, 31, 23, 39, 81, 61, 65, 47, 101}, {119, 58, 27, 70, 120, 0, 57, 17, 6, 35, 7, 94, 102, 108, 77, 117, 99, 82, 30, 67, 15, 95, 32, 79, 121, 59, 78, 40, 48, 81, 9, 3, 16, 72, 127, 41, 106, 112, 63, 13, 71, 47, 28, 60, 25, 11, 51, 19, 92, 76, 100, 87, 4, 122, 85, 90, 46, 105, 8, 24, 54, 123, 1, 89, 118, 52, 61, 96, 86, 83, 2, 33, 37, 114, 38, 111, 107, 62, 50, 126, 84, 73, 125, 44, 88, 45, 49, 116, 14, 68, 29, 65, 31, 110, 36, 39, 103, 109, 5, 10, 69, 124, 18, 21, 104, 43, 23, 93, 101, 74, 20, 56, 26, 22, 91, 34, 98, 42, 113, 80, 66, 53, 97, 12, 115, 55, 75, 64}, {121, 56, 78, 58, 8, 85, 79, 51, 77, 123, 42, 87, 15, 112, 38, 111, 23, 125, 12, 66, 45, 105, 35, 25, 24, 52, 60, 33, 0, 7, 115, 16, 100, 5, 44, 119, 18, 62, 89, 92, 13, 11, 3, 49, 99, 117, 86, 1, 127, 26, 22, 54, 84, 41, 98, 57, 61, 43, 97, 114, 74, 73, 9, 71, 122, 76, 102, 19, 120, 47, 116, 82, 91, 69, 37, 59, 113, 101, 67, 68, 28, 107, 118, 81, 14, 80, 72, 106, 94, 126, 109, 53, 83, 50, 10, 32, 17, 110, 29, 34, 2, 103, 21, 6, 20, 90, 46, 30, 36, 96, 88, 65, 4, 48, 27, 63, 75, 40, 55, 124, 64, 104, 70, 95, 108, 39, 31, 93}, {73, 36, 107, 43, 2, 28, 54, 31, 50, 93, 29, 0, 34, 48, 104, 22, 25, 92, 94, 96, 72, 5, 63, 98, 109, 125, 76, 23, 42, 99, 47, 11, 57, 84, 20, 69, 70, 120, 52, 83, 24, 39, 115, 26, 71, 60, 61, 85, 35, 95, 102, 21, 89, 117, 90, 67, 17, 81, 62, 113, 32, 37, 127, 38, 126, 82, 7, 10, 78, 41, 8, 87, 112, 106, 86, 88, 30, 44, 121, 97, 119, 6, 105, 40, 4, 64, 118, 101, 79, 123, 103, 53, 12, 124, 19, 9, 110, 80, 14, 108, 15, 100, 68, 1, 3, 91, 18, 27, 59, 55, 46, 65, 33, 111, 56, 13, 114, 66, 122, 75, 74, 45, 51, 16, 116, 58, 77, 49}, {0, 52, 44, 83, 40, 54, 28, 7, 51, 110, 20, 106, 35, 92, 67, 30, 60, 123, 38, 101, 4, 97, 26, 56, 111, 99, 70, 126, 8, 112, 95, 81, 117, 41, 90, 5, 55, 14, 109, 127, 74, 80, 21, 119, 64, 100, 42, 73, 94, 23, 48, 19, 82, 116, 33, 88, 9, 18, 46, 66, 108, 59, 114, 71, 34, 122, 39, 102, 118, 53, 16, 31, 125, 75, 62, 13, 32, 27, 86, 57, 87, 10, 76, 24, 107, 25, 36, 105, 72, 78, 2, 12, 47, 37, 91, 98, 43, 11, 103, 85, 63, 104, 1, 124, 15, 65, 93, 58, 17, 77, 84, 61, 121, 29, 45, 22, 115, 68, 3, 113, 89, 96, 49, 69, 120, 50, 6, 79}};


        private final byte[][][] ENCLAVE_TABLE = new byte[][][]{
                {{5, 4, 2, 1, 3}, {2, 3, 5, 4, 1}, {3, 1, 4, 5, 2}},
                {{3, 1, 2, 5, 4}, {5, 3, 4, 1, 2}, {2, 5, 1, 4, 3}},
                {{5, 4, 1, 3, 2}, {4, 3, 5, 2, 1}, {2, 1, 3, 5, 4}},
                {{5, 2, 1, 3, 4}, {4, 5, 3, 2, 1}, {2, 1, 5, 4, 3}},
                //
                {{3, 4, 2, 5, 1}, {1, 3, 5, 2, 4}, {2, 1, 4, 3, 5}},
                {{3, 5, 2, 4, 1}, {2, 1, 4, 3, 5}, {5, 4, 3, 1, 2}},
                {{4, 3, 5, 1, 2}, {2, 4, 1, 3, 5}, {1, 5, 4, 2, 3}},
                {{4, 5, 2, 3, 1}, {2, 3, 1, 5, 4}, {3, 1, 5, 4, 2}},

                {{4, 1, 3, 2, 5}, {1, 2, 5, 3, 4}, {3, 5, 1, 4, 2}},
                {{1, 4, 2, 3, 5}, {4, 5, 1, 2, 3}, {2, 3, 4, 5, 1}},
                {{2, 3, 4, 1, 5}, {5, 2, 3, 4, 1}, {3, 5, 1, 2, 4}},
                {{2, 4, 3, 5, 1}, {5, 3, 2, 1, 4}, {3, 5, 1, 4, 2}},
                {{1, 4, 2, 3, 5}, {2, 5, 3, 4, 1}, {4, 1, 5, 2, 3}},
                {{5, 4, 2, 3, 1}, {3, 5, 1, 4, 2}, {4, 2, 3, 1, 5}},
                {{2, 4, 1, 5, 3}, {4, 2, 5, 3, 1}, {5, 1, 3, 4, 2}},
                {{4, 2, 5, 3, 1}, {2, 5, 3, 1, 4}, {3, 4, 1, 2, 5}},
                {{2, 4, 5, 1, 3}, {5, 1, 2, 3, 4}, {3, 2, 4, 5, 1}},
                {{2, 4, 5, 1, 3}, {3, 2, 1, 4, 5}, {1, 5, 4, 3, 2}},
                {{4, 1, 2, 3, 5}, {2, 4, 3, 5, 1}, {1, 2, 5, 4, 3}},
                {{2, 1, 4, 3, 5}, {5, 4, 3, 2, 1}, {3, 5, 2, 1, 4}},
                {{1, 5, 2, 3, 4}, {4, 1, 3, 2, 5}, {3, 2, 4, 5, 1}},
                {{2, 4, 1, 5, 3}, {4, 5, 2, 3, 1}, {1, 3, 5, 4, 2}},
                {{2, 3, 1, 5, 4}, {3, 1, 5, 4, 2}, {4, 2, 3, 1, 5}},
                {{5, 3, 1, 4, 2}, {3, 5, 4, 2, 1}, {1, 2, 3, 5, 4}},
                {{1, 3, 5, 4, 2}, {5, 1, 4, 2, 3}, {4, 2, 3, 5, 1}},
                {{1, 2, 4, 3, 5}, {5, 1, 3, 4, 2}, {2, 3, 5, 1, 4}},
                {{3, 4, 1, 2, 5}, {5, 2, 3, 4, 1}, {2, 1, 4, 5, 3}},
                {{5, 3, 4, 1, 2}, {1, 4, 3, 2, 5}, {4, 2, 1, 5, 3}},
                {{2, 5, 3, 4, 1}, {5, 1, 4, 2, 3}, {1, 2, 5, 3, 4}},
                {{2, 1, 5, 4, 3}, {1, 5, 4, 3, 2}, {5, 2, 3, 1, 4}},
                {{5, 2, 3, 4, 1}, {4, 1, 2, 3, 5}, {2, 3, 5, 1, 4}},
                {{3, 4, 5, 2, 1}, {5, 3, 2, 1, 4}, {4, 2, 1, 5, 3}},
                {{2, 1, 3, 4, 5}, {5, 2, 4, 3, 1}, {4, 3, 1, 5, 2}},
                {{1, 5, 2, 3, 4}, {2, 3, 4, 5, 1}, {4, 1, 5, 2, 3}},
                {{4, 2, 1, 5, 3}, {2, 3, 5, 1, 4}, {5, 4, 3, 2, 1}},
                {{2, 5, 1, 3, 4}, {3, 2, 5, 4, 1}, {1, 4, 2, 5, 3}},
                {{1, 5, 2, 4, 3}, {3, 1, 5, 2, 4}, {2, 3, 4, 5, 1}},
                {{4, 1, 3, 5, 2}, {1, 3, 2, 4, 5}, {2, 5, 1, 3, 4}},
                {{4, 1, 5, 3, 2}, {5, 2, 3, 4, 1}, {1, 3, 2, 5, 4}},
                {{4, 3, 5, 2, 1}, {3, 1, 4, 5, 2}, {2, 5, 1, 3, 4}},
                {{1, 2, 3, 5, 4}, {4, 5, 1, 2, 3}, {3, 1, 5, 4, 2}},
                {{5, 2, 1, 3, 4}, {4, 3, 5, 1, 2}, {3, 5, 4, 2, 1}},
                {{5, 1, 3, 2, 4}, {2, 4, 1, 5, 3}, {3, 5, 2, 4, 1}},
                {{2, 3, 5, 1, 4}, {5, 4, 1, 2, 3}, {1, 2, 4, 3, 5}},
                {{5, 4, 1, 3, 2}, {1, 5, 2, 4, 3}, {2, 3, 4, 5, 1}},
                {{1, 2, 5, 4, 3}, {4, 3, 1, 5, 2}, {2, 1, 4, 3, 5}},
                {{2, 3, 1, 5, 4}, {4, 5, 2, 1, 3}, {5, 4, 3, 2, 1}},
                {{3, 2, 5, 4, 1}, {1, 4, 2, 3, 5}, {4, 3, 1, 5, 2}},
                {{4, 5, 2, 1, 3}, {1, 4, 3, 2, 5}, {2, 3, 1, 5, 4}},
                {{2, 4, 5, 3, 1}, {5, 2, 1, 4, 3}, {3, 5, 4, 1, 2}},
                {{5, 2, 4, 1, 3}, {3, 1, 5, 4, 2}, {2, 4, 1, 3, 5}},
                {{5, 4, 3, 1, 2}, {1, 5, 2, 4, 3}, {4, 2, 1, 3, 5}},
                {{2, 1, 5, 3, 4}, {3, 4, 2, 1, 5}, {1, 2, 4, 5, 3}},
                {{4, 5, 2, 3, 1}, {2, 3, 5, 1, 4}, {5, 1, 4, 2, 3}},
                {{3, 2, 4, 5, 1}, {2, 4, 3, 1, 5}, {4, 5, 1, 2, 3}},
                {{5, 3, 4, 2, 1}, {2, 1, 5, 4, 3}, {4, 5, 1, 3, 2}},
                {{5, 3, 2, 1, 4}, {3, 2, 1, 4, 5}, {2, 5, 4, 3, 1}},
                {{2, 5, 4, 1, 3}, {3, 1, 2, 4, 5}, {4, 2, 5, 3, 1}},
                {{4, 2, 1, 3, 5}, {3, 4, 5, 2, 1}, {1, 5, 2, 4, 3}},
                {{4, 1, 3, 2, 5}, {2, 5, 4, 1, 3}, {5, 2, 1, 3, 4}},
                {{1, 5, 3, 2, 4}, {2, 4, 5, 3, 1}, {5, 2, 4, 1, 3}},
                {{3, 5, 2, 4, 1}, {5, 3, 4, 1, 2}, {1, 4, 3, 2, 5}},
                {{5, 1, 4, 2, 3}, {3, 4, 2, 5, 1}, {2, 5, 3, 1, 4}},
                {{2, 5, 1, 3, 4}, {3, 1, 2, 4, 5}, {5, 3, 4, 2, 1}},
                {{2, 4, 1, 3, 5}, {4, 5, 2, 1, 3}, {1, 2, 3, 5, 4}},
                {{2, 3, 4, 1, 5}, {3, 4, 5, 2, 1}, {1, 5, 2, 4, 3}},
                {{2, 1, 5, 3, 4}, {1, 4, 3, 2, 5}, {5, 3, 2, 4, 1}},
                {{5, 3, 4, 2, 1}, {1, 2, 5, 3, 4}, {3, 1, 2, 4, 5}},
                {{4, 3, 5, 2, 1}, {3, 2, 1, 4, 5}, {5, 4, 2, 1, 3}},
                {{2, 4, 5, 3, 1}, {3, 5, 1, 2, 4}, {5, 1, 3, 4, 2}},
                {{5, 1, 3, 2, 4}, {2, 3, 5, 4, 1}, {1, 2, 4, 5, 3}},
                {{5, 3, 1, 2, 4}, {3, 5, 4, 1, 2}, {4, 1, 2, 5, 3}},
                {{2, 4, 1, 5, 3}, {4, 3, 2, 1, 5}, {1, 5, 4, 3, 2}},
                {{3, 1, 4, 2, 5}, {1, 5, 3, 4, 2}, {4, 3, 2, 5, 1}},
                {{5, 2, 4, 3, 1}, {1, 5, 3, 4, 2}, {4, 1, 2, 5, 3}},
                {{5, 4, 1, 3, 2}, {3, 1, 4, 2, 5}, {1, 3, 2, 5, 4}},
                {{2, 4, 3, 5, 1}, {5, 3, 4, 1, 2}, {3, 5, 1, 2, 4}},
                {{4, 3, 1, 2, 5}, {1, 4, 3, 5, 2}, {3, 2, 5, 1, 4}},
                {{2, 1, 4, 5, 3}, {5, 3, 1, 4, 2}, {3, 2, 5, 1, 4}},
                {{4, 3, 5, 2, 1}, {5, 2, 1, 4, 3}, {2, 5, 3, 1, 4}},
                {{3, 2, 1, 5, 4}, {1, 4, 5, 2, 3}, {4, 5, 2, 3, 1}},
                {{4, 3, 2, 1, 5}, {2, 1, 3, 5, 4}, {1, 4, 5, 3, 2}},
                {{4, 2, 1, 3, 5}, {2, 1, 3, 5, 4}, {1, 4, 5, 2, 3}},
                {{3, 5, 2, 1, 4}, {4, 1, 3, 5, 2}, {2, 4, 1, 3, 5}},
                {{2, 3, 5, 1, 4}, {4, 5, 1, 3, 2}, {5, 1, 2, 4, 3}},
                {{5, 3, 4, 1, 2}, {2, 4, 5, 3, 1}, {3, 2, 1, 5, 4}},
                {{4, 5, 2, 1, 3}, {5, 4, 3, 2, 1}, {3, 2, 1, 5, 4}},
                {{1, 2, 3, 4, 5}, {3, 1, 2, 5, 4}, {2, 4, 5, 1, 3}},
                {{3, 5, 1, 4, 2}, {1, 4, 5, 2, 3}, {4, 2, 3, 5, 1}},
                {{4, 5, 2, 1, 3}, {5, 2, 3, 4, 1}, {1, 3, 5, 2, 4}},
                {{1, 2, 5, 4, 3}, {3, 5, 1, 2, 4}, {4, 1, 2, 3, 5}},
                {{2, 5, 3, 1, 4}, {5, 3, 4, 2, 1}, {1, 4, 2, 5, 3}},
                {{2, 1, 4, 5, 3}, {3, 4, 5, 2, 1}, {5, 3, 2, 1, 4}},
                {{4, 3, 5, 1, 2}, {1, 2, 4, 3, 5}, {5, 1, 2, 4, 3}},
                {{5, 2, 4, 1, 3}, {2, 3, 1, 4, 5}, {3, 1, 5, 2, 4}},
                {{1, 4, 2, 3, 5}, {3, 1, 4, 5, 2}, {2, 5, 1, 4, 3}},
                {{2, 5, 3, 4, 1}, {1, 4, 2, 5, 3}, {5, 3, 4, 1, 2}},
                {{3, 2, 1, 5, 4}, {5, 3, 4, 1, 2}, {2, 1, 3, 4, 5}},
                {{3, 1, 2, 5, 4}, {2, 4, 5, 1, 3}, {5, 2, 3, 4, 1}},
                {{3, 4, 1, 2, 5}, {2, 1, 5, 4, 3}, {1, 5, 4, 3, 2}},
                {{4, 3, 1, 5, 2}, {5, 1, 3, 2, 4}, {2, 4, 5, 3, 1}},
                {{4, 5, 2, 3, 1}, {2, 3, 1, 5, 4}, {1, 4, 3, 2, 5}},
                {{2, 3, 1, 5, 4}, {5, 2, 4, 1, 3}, {3, 4, 5, 2, 1}},
                {{4, 1, 2, 3, 5}, {3, 4, 5, 1, 2}, {1, 5, 3, 2, 4}},
                {{4, 1, 2, 5, 3}, {2, 4, 3, 1, 5}, {5, 2, 4, 3, 1}},
                {{5, 2, 1, 4, 3}, {4, 1, 3, 2, 5}, {3, 4, 5, 1, 2}},
                {{3, 5, 2, 4, 1}, {2, 3, 1, 5, 4}, {1, 4, 5, 3, 2}},
                {{1, 5, 3, 4, 2}, {4, 2, 1, 5, 3}, {5, 3, 2, 1, 4}},
                {{2, 4, 1, 5, 3}, {1, 2, 4, 3, 5}, {5, 3, 2, 4, 1}},
                {{2, 4, 1, 3, 5}, {5, 1, 3, 4, 2}, {1, 3, 5, 2, 4}},
                {{3, 1, 2, 5, 4}, {2, 3, 4, 1, 5}, {5, 2, 1, 4, 3}},
                {{4, 2, 1, 5, 3}, {5, 4, 3, 1, 2}, {2, 5, 4, 3, 1}},
                {{3, 5, 4, 1, 2}, {1, 3, 2, 4, 5}, {5, 4, 1, 2, 3}},
                {{5, 2, 4, 3, 1}, {4, 3, 2, 1, 5}, {3, 1, 5, 2, 4}},
                {{4, 3, 1, 2, 5}, {1, 2, 5, 3, 4}, {3, 5, 2, 4, 1}},
                {{3, 5, 4, 2, 1}, {4, 2, 1, 5, 3}, {5, 1, 3, 4, 2}},
                {{4, 5, 1, 3, 2}, {5, 1, 3, 2, 4}, {3, 4, 2, 5, 1}},
                {{4, 2, 5, 1, 3}, {3, 5, 4, 2, 1}, {2, 1, 3, 4, 5}},
                {{2, 4, 5, 1, 3}, {3, 5, 2, 4, 1}, {1, 2, 4, 3, 5}},
                {{3, 2, 4, 5, 1}, {4, 1, 3, 2, 5}, {2, 4, 5, 1, 3}},
                {{2, 3, 5, 1, 4}, {4, 5, 1, 2, 3}, {1, 4, 3, 5, 2}},
                {{2, 4, 3, 5, 1}, {4, 1, 5, 2, 3}, {3, 2, 4, 1, 5}},
                {{1, 2, 4, 5, 3}, {5, 4, 3, 2, 1}, {3, 1, 2, 4, 5}},
                {{4, 3, 1, 2, 5}, {1, 5, 4, 3, 2}, {5, 2, 3, 4, 1}},
                {{5, 1, 3, 4, 2}, {3, 2, 1, 5, 4}, {1, 4, 5, 2, 3}},
                {{4, 2, 1, 3, 5}, {1, 3, 5, 4, 2}, {5, 1, 3, 2, 4}},
                {{4, 3, 1, 5, 2}, {2, 5, 4, 3, 1}, {3, 4, 2, 1, 5}},
                {{1, 3, 4, 2, 5}, {5, 1, 2, 3, 4}, {2, 4, 1, 5, 3}},

        };

    }
}