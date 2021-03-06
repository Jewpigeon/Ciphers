package Ciphers.HashFunctions.SHA2;


import Ciphers.HashFunctions.SHA;
import Ciphers.Utils.BitUtil;

public class SHA224 extends SHA {
    protected static final int LENGTH = 28;

    public SHA224() {
        super.LENGTH = LENGTH;
    }

    @Override
    public byte[] process(byte[] input) {
        final int[] K = new int[]{
                0x428A2F98, 0x71374491, 0xB5C0FBCF, 0xE9B5DBA5, 0x3956C25B, 0x59F111F1, 0x923F82A4, 0xAB1C5ED5,
                0xD807AA98, 0x12835B01, 0x243185BE, 0x550C7DC3, 0x72BE5D74, 0x80DEB1FE, 0x9BDC06A7, 0xC19BF174,
                0xE49B69C1, 0xEFBE4786, 0x0FC19DC6, 0x240CA1CC, 0x2DE92C6F, 0x4A7484AA, 0x5CB0A9DC, 0x76F988DA,
                0x983E5152, 0xA831C66D, 0xB00327C8, 0xBF597FC7, 0xC6E00BF3, 0xD5A79147, 0x06CA6351, 0x14292967,
                0x27B70A85, 0x2E1B2138, 0x4D2C6DFC, 0x53380D13, 0x650A7354, 0x766A0ABB, 0x81C2C92E, 0x92722C85,
                0xA2BFE8A1, 0xA81A664B, 0xC24B8B70, 0xC76C51A3, 0xD192E819, 0xD6990624, 0xF40E3585, 0x106AA070,
                0x19A4C116, 0x1E376C08, 0x2748774C, 0x34B0BCB5, 0x391C0CB3, 0x4ED8AA4A, 0x5B9CCA4F, 0x682E6FF3,
                0x748F82EE, 0x78A5636F, 0x84C87814, 0x8CC70208, 0x90BEFFFA, 0xA4506CEB, 0xBEF9A3F7, 0xC67178F2
        };
        int h0 = 0xC1059ED8;
        int h1 = 0x367CD507;
        int h2 = 0x3070DD17;
        int h3 = 0xF70E5939;
        int h4 = 0xFFC00B31;
        int h5 = 0x68581511;
        int h6 = 0x64F98FA7;
        int h7 = 0xBEFA4FA4;
        byte[] input_prepared = padding_process(input);
        for (int i = 0; i < input_prepared.length; i += 64) {

            int[] words_32bit = new int[64];
            byte[] chunk = new byte[64];
            System.arraycopy(input_prepared, i, chunk, 0, chunk.length);

            int[] chunk_L = BitUtil.ByteArrays.byteArrayToIntArray(chunk);
            System.arraycopy(chunk_L, 0, words_32bit, 0, chunk_L.length);

            int s0, s1;
            byte i_num = 64;

            for (int j = 16; j < i_num; j++) {
                s0 = (BitUtil.Rotation.rotateR(words_32bit[j - 15], 7)) ^ (BitUtil.Rotation.rotateR(words_32bit[j - 15], 18)) ^ (words_32bit[j - 15] >>> 3);
                s1 = (BitUtil.Rotation.rotateR(words_32bit[j - 2], 17)) ^ (BitUtil.Rotation.rotateR(words_32bit[j - 2], 19)) ^ (words_32bit[j - 2] >>> 10);

                words_32bit[j] = words_32bit[j - 16] + s0 + words_32bit[j - 7] + s1;
            }

            int a = h0;
            int b = h1;
            int c = h2;
            int d = h3;
            int e = h4;
            int f = h5;
            int g = h6;
            int h = h7;

            for (int q = 0; q < i_num; q++) {

                int S0 = (BitUtil.Rotation.rotateR(a, 2)) ^ (BitUtil.Rotation.rotateR(a, 13)) ^ (BitUtil.Rotation.rotateR(a, 22));
                int S1 = (BitUtil.Rotation.rotateR(e, 6)) ^ (BitUtil.Rotation.rotateR(e, 11)) ^ (BitUtil.Rotation.rotateR(e, 25));

                int ch = (e & f) ^ ((~e) & g);
                int maj = (a & b) ^ (a & c) ^ (b & c);


                int temp1 = h + S1 + ch + K[q] + words_32bit[q];
                int temp2 = S0 + maj;

                h = g;
                g = f;
                f = e;
                e = d + temp1;
                d = c;
                c = b;
                b = a;
                a = temp1 + temp2;
            }

            h0 = h0 + a;
            h1 = h1 + b;
            h2 = h2 + c;
            h3 = h3 + d;
            h4 = h4 + e;
            h5 = h5 + f;
            h6 = h6 + g;
            h7 = h7 + h;


        }


        byte[] hash = BitUtil.ByteArrays.intArrayToByteArray(
                new int[]{
                        h0,
                        h1,
                        h2,
                        h3,
                        h4,
                        h5,
                        h6,
                });
        return hash;
    }


    @Override
    protected byte[] padding_process(byte[] input) {
        int l_orig = input.length;
        int l = l_orig << 3;
        int k = 2;
        while ((l + k) % 512 != 448) k++;
        l += k + 64;
        byte[] prepared = new byte[l >> 3];
        System.arraycopy(input, 0, prepared, 0, l_orig);
        prepared[l_orig] = (byte) 0b10000000;
        final byte[] coping = BitUtil.ByteArrays.intToByteArray(l_orig << 3);
        System.arraycopy(coping, 0, prepared, prepared.length - coping.length, coping.length);
        return prepared;
    }
}
