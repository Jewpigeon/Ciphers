package Utils;

public  class bitUtil {

    private static final int BYTE_BITSIZE = 8;
    private static final int SHORT_BITSIZE = 16;
    private static final int INT_BITSIZE = 32;
    private static final int LONG_BITSIZE = 64;

    public static long rotateR(long i, long bits) {
        return ((i >>> bits) | (i << (LONG_BITSIZE - bits)));
    }

    public static long rotateL(long i, long bits) {
        return ((i << bits) | (i >>> (LONG_BITSIZE - bits)));
    }

    public static int rotateR(int i, int bits) {
        return ((i >>> bits) | (i << (INT_BITSIZE - bits)));
    }

    public static int rotateL(int i, int bits) {
        return ((i << bits) | (i >>> (INT_BITSIZE - bits)));
    }


    public static short rotateR(short i, int bits) {
        return (short) ((i >>> bits) | (i << (SHORT_BITSIZE - bits)));
    }

    public static short rotateL(short i, int bits) {
        return (short) ((i << bits) | (i >>> (SHORT_BITSIZE - bits)));
    }

    public static byte rotateR(byte i, int bits) {
        return (byte) ((i >>> bits) | (i << (BYTE_BITSIZE - bits)));

    }

    public static byte rotateL(byte i, int bits) {
        return (byte) ((i << bits) | (i >>> (BYTE_BITSIZE - bits)));
    }


        public static long byteArrayToLong(byte[] b) {

            long l = b[0] & 0xff;
            l = (l << 8) ^ (long) b[1] & 0xff;
            l = (l << 8) ^ (long) b[2] & 0xff;
            l = (l << 8) ^ (long) b[3] & 0xff;
            l = (l << 8) ^ (long) b[4] & 0xff;
            l = (l << 8) ^ (long) b[5] & 0xff;
            l = (l << 8) ^ (long) b[6] & 0xff;
            l = (l << 8) ^ (long) b[7] & 0xff;

            return l;
        }


        public static byte[] longToByteArray(long l) {
            return new byte[]{
                    (byte) (l >> 56),
                    (byte) (l >> 48),
                    (byte) (l >> 40),
                    (byte) (l >> 32),
                    (byte) (l >> 24),
                    (byte) (l >> 16),
                    (byte) (l >> 8),
                    (byte) l
            };
        }

        public static long[] byteArrayToLongArray(byte[] b) {
            long[] l = new long[b.length>>3];


            for (int i = 0,j=0; i < b.length; i+=8,j++) {
                byte[] chunk = new byte[8];
                System.arraycopy(b,i,chunk,0,chunk.length);
                l[j] = byteArrayToLong(chunk);

            }
            return l;
        }

        public static byte[] longArrayToByteArray(long[] l) {
            byte[] b = new byte[l.length<<3];
            int padding_t = 0;
            for (int i = 0; i < l.length; i++) {
                byte[] chunk = bitUtil.longToByteArray(l[i]);
                System.arraycopy(chunk,0,b,i+padding_t,chunk.length);
                padding_t+=chunk.length-1;

            }
            return b;
        }

        public static int byteArrayToInt(byte[] b) {
            int i = b[0] & 0xff;
            i = (i << 8) ^ (int) b[1] & 0xff;
            i = (i << 8) ^ (int) b[2] & 0xff;
            i = (i << 8) ^ (int) b[3] & 0xff;

            return i;
        }





        public static byte[] intToByteArray(int i) {
            return new byte[]{
                    (byte) (i >> 24),
                    (byte) (i >> 16),
                    (byte) (i >> 8),
                    (byte)  i
            };
        }



        public static int[] byteArrayToIntArray(byte[] b) {
            int[] i_32 = new int[b.length>>2];


            for (int i = 0,j=0; i < b.length; i+=4,j++) {
                byte[] chunk = new byte[4];
                System.arraycopy(b,i,chunk,0,chunk.length);
                i_32[j] = byteArrayToInt(chunk);

            }
            return i_32;
        }

        public static byte[] intArrayToByteArray(int[] i_32) {
            byte[] b = new byte[i_32.length<<2];
            int padding_t = 0;
            for (int i = 0; i < i_32.length; i++) {
                byte[] chunk = bitUtil.intToByteArray(i_32[i]);
                System.arraycopy(chunk,0,b,i+padding_t,chunk.length);
                padding_t+=chunk.length-1;

            }
            return b;
        }


    public static short byteArrayToShort(byte[] b) {
        short s = (short) (b[0] & 0xff);
        s = (short) ((s << 8) ^ (int) b[1] & 0xff);
        return s;
    }





    public static byte[] shortToByteArray(short i) {
        return new byte[]{
                (byte) (i >> 8),
                (byte)  i
        };
    }



    public static short[] byteArrayToShortArray(byte[] b) {
        short[] s = new short[b.length>>1];


        for (int i = 0,j=0; i < b.length; i+=2,j++) {
            byte[] chunk = new byte[2];
            System.arraycopy(b,i,chunk,0,chunk.length);
            s[j] = byteArrayToShort(chunk);

        }
        return s;
    }

    public static byte[] shortArrayToByteArray(short[] s) {
        byte[] b = new byte[s.length<<1];
        int padding_t = 0;
        for (int i = 0; i < s.length; i++) {
            byte[] chunk = bitUtil.shortToByteArray(s[i]);
            System.arraycopy(chunk,0,b,i+padding_t,chunk.length);
            padding_t+=chunk.length-1;

        }
        return b;
    }

    public static void printHex_byteArray(byte[] input){
        for (byte b:input) {
            System.out.printf("%x",b);
        }
        System.out.println();
    }

    public static void printHex_byteArray(byte[] input,boolean padding){
        for (byte b:input) {
            System.out.printf("%x",b);
        }
        System.out.println();
    }
    public static void printBinary_byteArray(byte[] input){
        for (byte b:input) {
            System.out.print(Integer.toBinaryString(b));
        }
        System.out.println();
    }

    public static byte[][] splitTo4bytes(byte[] input) {
        byte[][] retq = new byte[input.length >> 2][4];
        for (int i = 0,j=0; i < input.length; i+=4,j++) {
            if(i+4 > input.length) break;
            System.arraycopy(input,i,retq[j],0,4);
        }
        return retq;
    }

    public static byte[][] splitTo8bytes(byte[] input) {
        byte[][] retq = new byte[input.length >> 3][8];
        for (int i = 0,j=0; i < input.length; i+=8,j++) {
            if(i+8 > input.length) break;
            System.arraycopy(input,i,retq[j],0,8);
        }
        return retq;
    }

    public static byte[][] splitTo16bytes(byte[] input) {
        byte[][] retq = new byte[input.length >> 4][16];
        for (int i = 0,j=0; i < input.length; i+=16,j++) {
            if(i+16 > input.length) break;
            System.arraycopy(input,i,retq[j],0,16);
        }
        return retq;
    }


    public static byte[] splitBy4bits(byte[] input) {
        byte[] input_4bit = new byte[input.length<<1];
        for (int i = 0,j=0; i < input.length; i++,j++) {
            input_4bit[j] = (byte) ((input[i]>>4) & 0b1111);
            input_4bit[j+1] = (byte) ((input[i]&0b1111));
           j++;
        }
        return input_4bit;
    }
    public static byte[] concatBy4bit(byte[] input_4bit) {
        byte[] input = new byte[input_4bit.length>>1];
        for (int i = 0,j=0; i < input_4bit.length; i++,j++) {
            input[j] = (byte)( (input_4bit[i] << 4) | input_4bit[i+1]);
            i++;
        }
        return input;
    }



    public static int extendToSize(int variable, int size){
        if (variable%size==0) return variable;
        return (variable - (variable%size) +size);
    }
    public static byte[] xor(byte[] c, byte[] b){
        byte[] a = c.clone();
        for (int i = 0; i < a.length; i++) {
            a[i]^=b[i];
        }
        return a;
    }

}
