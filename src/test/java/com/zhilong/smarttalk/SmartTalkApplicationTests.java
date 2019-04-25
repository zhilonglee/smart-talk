package com.zhilong.smarttalk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.BitSet;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SmartTalkApplicationTests {

    @Test
    public void contextLoads() {

    }

    public static void main(String[] args) {

        // create 2 bitsets
        BitSet bitset1 = new BitSet(8);
        BitSet bitset2 = new BitSet(8);

        // assign values to bitset1
        bitset1.set(0);
        bitset1.set(1);
        bitset1.set(2);
        bitset1.set(3);
        bitset1.set(4);
        bitset1.set(5);

        // assign values to bitset2
        bitset2.set(2);
        bitset2.set(4);
        bitset2.set(6);
        bitset2.set(8);
        bitset2.set(10);

        // print the sets
        System.out.println("Bitset1:" + bitset1);
        System.out.println("Bitset2:" + bitset2);

        // get index 1 to 4 of bitset1
        System.out.println("" + bitset1.get(1, 4));

        // get index 2 to 10 of bitset2
        System.out.println("" + bitset2.get(2, 10));

    }

}
