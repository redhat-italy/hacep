package it.redhat.hacep.rules;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

public class TestStream {

    @Test
    public void testInputOutput() throws IOException {
        byte[] buffer;
        byte[] buf1 = new byte[207];
        byte[] buf2 = new byte[662];
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.putInt(buf1.length);
        byteBuffer.put(buf1);
        byteBuffer.putInt(buf2.length);
        byteBuffer.put(buf2);
        buffer = byteBuffer.array();

        ByteBuffer wrap = ByteBuffer.wrap(buffer);
        int read = wrap.getInt();
        Assert.assertEquals(buf1.length, read);
        byte[] r_buf1 = new byte[read];
        wrap.get(r_buf1);
        read = wrap.getInt();
        Assert.assertEquals(buf2.length, read);
        byte[] r_buf2 = new byte[read];
        wrap.get(r_buf2);
    }
}
