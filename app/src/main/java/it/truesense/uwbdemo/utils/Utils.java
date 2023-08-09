package it.truesense.uwbdemo.utils;

//import com.google.common.base.Ascii;

public class Utils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static byte[] byteToByteArray(byte b) {
        return new byte[]{(byte) (b & 255)};
    }

    public static byte[] intToByteArray(int i) {
        byte[] bArr = new byte[4];
        bArr[3] = (byte) (i & 255);
        bArr[2] = (byte) ((i >> 8) & 255);
        bArr[1] = (byte) ((i >> 16) & 255);
        bArr[0] = (byte) ((i >> 24) & 255);
        return bArr;
    }

    public static byte[] shortToByteArray(short s) {
        byte[] bArr = new byte[2];
        bArr[1] = (byte) (s & 255);
        bArr[0] = (byte) ((s >> 8) & 255);
        return bArr;
    }


    public static int byteArrayToInt32(byte[] bArr) {
        int i;
        byte b;
        if (bArr.length == 1) {
            return bArr[0] & 255;
        }
        if (bArr.length == 2) {
            i = (bArr[0] & 255) << 8;
            b = bArr[1];
        } else if (bArr.length == 3) {
            i = ((bArr[0] & 255) << 16) + ((bArr[1] & 255) << 8);
            b = bArr[2];
        } else if (bArr.length == 4) {
            i = (bArr[0] << 24) + ((bArr[1] & 255) << 16) + ((bArr[2] & 255) << 8);
            b = bArr[3];
        } else {
            throw new IndexOutOfBoundsException();
        }
        return i + (b & 255);
    }

    public static short byteArrayToInt16(byte[] bArr) {
        if (bArr.length == 1) {
            return (short) (bArr[0] & 255);
        }
        if (bArr.length == 2) {
            return (short) (((bArr[0] & 255) << 8) + (bArr[1] & 255));
        }
        throw new IndexOutOfBoundsException();
    }

    public static byte byteArrayToInt8(byte[] bArr) {
        if (bArr.length == 1) {
            return (byte) (bArr[0] & 255);
        }
        throw new IndexOutOfBoundsException();
    }

    public static boolean compareByteArrays(byte[] bArr, int i, byte[] bArr2, int i2, int i3) {
        int i4 = i;
        int i5 = i2;
        while (i5 < i3) {
            if (bArr[i4] != bArr2[i5]) {
                return false;
            }
            i4++;
            i5++;
        }
        return i4 - i == i3 && i5 - i2 == i3;
    }



    public static byte[] slice(byte[] bArr, int i, int i2) {
        byte[] bArr2 = new byte[i];
        System.arraycopy(bArr, i2, bArr2, 0, i);
        return bArr2;
    }

    

    public static byte[] invert(byte[] bArr) {
        int length = bArr.length;
        byte[] bArr2 = new byte[length];
        for (int i = 0; i < length; i++) {
            bArr2[i] = bArr[(length - 1) - i];
        }
        return bArr2;
    }

    public static byte[] concat(byte[] bArr, byte[] bArr2) {
        if (bArr == null) {
            return bArr2;
        }
        if (bArr2 == null) {
            return bArr;
        }
        byte[] bArr3 = new byte[(bArr.length + bArr2.length)];
        System.arraycopy(bArr, 0, bArr3, 0, bArr.length);
        System.arraycopy(bArr2, 0, bArr3, bArr.length, bArr2.length);
        return bArr3;
    }

    public static byte[] concat(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        if (bArr == null) {
            return concat(bArr2, bArr3);
        }
        if (bArr2 == null) {
            return concat(bArr, bArr3);
        }
        if (bArr3 == null) {
            return concat(bArr, bArr2);
        }
        byte[] bArr4 = new byte[(bArr.length + bArr2.length + bArr3.length)];
        System.arraycopy(bArr, 0, bArr4, 0, bArr.length);
        System.arraycopy(bArr2, 0, bArr4, bArr.length, bArr2.length);
        System.arraycopy(bArr3, 0, bArr4, bArr.length + bArr2.length, bArr3.length);
        return bArr4;
    }

    public static byte[] concat(byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4) {
        if (bArr == null) {
            return concat(bArr2, bArr3, bArr4);
        }
        if (bArr2 == null) {
            return concat(bArr, bArr3, bArr4);
        }
        if (bArr3 == null) {
            return concat(bArr, bArr2, bArr4);
        }
        if (bArr4 == null) {
            return concat(bArr, bArr2, bArr3);
        }
        byte[] bArr5 = new byte[(bArr.length + bArr2.length + bArr3.length + bArr4.length)];
        System.arraycopy(bArr, 0, bArr5, 0, bArr.length);
        System.arraycopy(bArr2, 0, bArr5, bArr.length, bArr2.length);
        System.arraycopy(bArr3, 0, bArr5, bArr.length + bArr2.length, bArr3.length);
        System.arraycopy(bArr4, 0, bArr5, bArr.length + bArr2.length + bArr3.length, bArr4.length);
        return bArr5;
    }
}
