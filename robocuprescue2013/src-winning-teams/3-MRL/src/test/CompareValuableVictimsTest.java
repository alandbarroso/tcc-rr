package test;

import mrl.ambulance.structures.ValuableVictim;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.fail;

/**
 * User: pooyad
 * Date: 6/21/11
 * Time: 2:38 PM
 */
public class CompareValuableVictimsTest {
    List<ValuableVictim> valuableVictims=new ArrayList<ValuableVictim>();

    @Before
    public void prepare() {

        ValuableVictim valuableVictim;
        Random rnd1=new Random(System.currentTimeMillis()+192);
        Random rnd2=new Random(System.currentTimeMillis()+2341);
        Random rnd3=new Random(System.currentTimeMillis()+236);

        for(int i=0;i<100;i++){
//            valuableVictim=new
//                    ValuableVictim(new EntityID(rnd1.nextInt(50)),rnd2.nextInt(50),rnd3.nextInt(50));
//            valuableVictims.add(valuableVictim);
        }

    }

    @Test
    public void SortmasalanchichiTest() {

//        Collections.sort(valuableVictims, ConstantComparators.VALUABLE_VICTIME_COMPARATOR);

        for(int i=1;i<100;i++){

            if(valuableVictims.get(i-1).getCaop()>valuableVictims.get(i).getCaop()){
                fail();
            }else if(valuableVictims.get(i-1).getCaop()==valuableVictims.get(i).getCaop()){

//                if(valuableVictims.get(i-1).getBrd()>valuableVictims.get(i).getBrd()){
//                    fail();
//                }
            }

        }

    }
}
