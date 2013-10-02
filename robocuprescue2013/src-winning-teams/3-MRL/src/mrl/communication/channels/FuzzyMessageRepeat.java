package mrl.communication.channels;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 21, 2011
 * Time: 8:29:00 PM
 */
public class FuzzyMessageRepeat {

    // priority : low(l) , medium(m) , high(h) , very high(v) .
// real bandwidth : small-256-(s) , low-512-(l) , medium-1024-(m) , high-2048-(h) , big-4096(b) .
// channel noise  : none-<0.01-(n) , tiny-0.02-(t) , veryLow-0.05-(v) , low-0.1-(l) , medium-0.15-(m) , high-0.2-(h) , extraHigh-high-0.3-(e) .
    private static String[] rules = new String[]{
            "lsn", "lst", "lsv", "lsl", "lsm", "lsh", "lse",
            "lln", "llt", "llv", "lll", "llm", "llh", "lle",
            "lmn", "lmt", "lmv", "lml", "lmm", "lmh", "lme",
            "lhn", "lht", "lhv", "lhl", "lhm", "lhh", "lhe",
            "lbn", "lbt", "lbv", "lbl", "lbm", "lbh", "lbe",
            "msn", "mst", "msv", "msl", "msm", "msh", "mse",
            "mln", "mlt", "mlv", "mll", "mlm", "mlh", "mle",
            "mmn", "mmt", "mmv", "mml", "mmm", "mmh", "mme",
            "mhn", "mht", "mhv", "mhl", "mhm", "mhh", "mhe",
            "mbn", "mbt", "mbv", "mbl", "mbm", "mbh", "mbe",
            "hsn", "hst", "hsv", "hsl", "hsm", "hsh", "hse",
            "hln", "hlt", "hlv", "hll", "hlm", "hlh", "hle",
            "hmn", "hmt", "hmv", "hml", "hmm", "hmh", "hme",
            "hhn", "hht", "hhv", "hhl", "hhm", "hhh", "hhe",
            "hbn", "vbt", "vbv", "vbl", "vbm", "vbh", "vbe",
            "vsn", "vst", "vsv", "vsl", "vsm", "vsh", "vse",
            "vln", "vlt", "vlv", "vll", "vlm", "vlh", "vle",
            "vmn", "vmt", "vmv", "vml", "vmm", "vmh", "vme",
            "vhn", "vht", "vhv", "vhl", "vhm", "vhh", "vhe",
            "vbn", "vbt", "vbv", "vbl", "vbm", "vbh", "vbe"};

    private static char[] stateTable = new char[]{
            '1', '0', '0', '0', '0', '0', '0',
            '1', '1', '1', '0', '0', '0', '0',
            '1', '1', '1', '1', '1', '1', '1',
            '1', '1', '1', '1', '1', '1', '1',
            '1', '1', '1', '1', '1', '1', '1',
            '1', '1', '1', '1', '1', '1', '1',
            '1', '1', '1', '2', '2', '2', '1',
            '1', '1', '2', '2', '2', '2', '2',
            '1', '1', '2', '2', '2', '2', '2',
            '1', '2', '2', '2', '2', '2', '3',
            '1', '1', '1', '1', '1', '1', '2',
            '1', '1', '2', '2', '2', '2', '3',
            '1', '2', '2', '3', '3', '3', '3',
            '1', '2', '3', '3', '3', '3', '4',
            '1', '2', '3', '3', '3', '4', '5',
            '1', '1', '1', '1', '1', '2', '2',
            '1', '1', '2', '2', '2', '2', '3',
            '1', '1', '2', '2', '2', '3', '3',
            '1', '2', '2', '2', '3', '3', '4',
            '2', '2', '3', '3', '4', '4', '5'
    };

    private static char getStateTable(int index) {
        return stateTable[index];
    }

    private static String getRules(int index) {
        return rules[index];
    }

    /**
     * @param rule: fuzzy rule
     * @param data: data[0]:priority, data[1]:RBW, data[2]:noise
     * @return membership
     */
    private static float getMax(String rule, float[] data) {
        float result = 1;
        float membershipValue;
        boolean doSetToOne;

        for (int i = 0; i < rule.length(); i++) {
            doSetToOne = false;
            membershipValue = 0;
            if (i == 0) {
                switch (rule.charAt(0)) {
                    case 'l':
                        if (data[i] == 1) {
                            membershipValue = 1;
                        } else {
                            membershipValue = 0;
                        }
                        break;

                    case 'm':
                        if (data[i] == 2) {
                            membershipValue = 1;
                        } else {
                            membershipValue = 0;
                        }
                        break;

                    case 'h':
                        if (data[i] == 3) {
                            membershipValue = 1;
                        } else {
                            membershipValue = 0;
                        }
                        break;
                    case 'v':
                        if (data[i] == 4) {
                            membershipValue = 1;
                        } else {
                            membershipValue = 0;
                        }
                        break;
                }
            } else if (i == 1) {
                switch (rule.charAt(i)) {
                    case 's':
                        if (data[i] < 0) {
                            doSetToOne = true;
                        } else if (data[i] <= 256) {
                            membershipValue = 1;
                        } else if (data[i] < 512) {
                            membershipValue = Math.abs((512 - data[i]) / 265);
                        } else {
                            membershipValue = 0;
                        }
                        break;

                    case 'l':
                        if (data[i] <= 256) {
                            membershipValue = 0;
                        } else if (data[i] <= 512) {
                            membershipValue = Math.abs((256 - data[i]) / 256);
                        } else if (data[i] < 1024) {
                            membershipValue = Math.abs((1024 - data[i]) / 512);
                        } else {
                            membershipValue = 0;
                        }
                        break;

                    case 'm':
                        if (data[i] <= 512) {
                            membershipValue = 0;
                        } else if (data[i] <= 1024) {
                            membershipValue = Math.abs((512 - data[i]) / 512);
                        } else if (data[i] < 2048) {
                            membershipValue = Math.abs((2048 - data[i]) / 1024);
                        } else {
                            membershipValue = 0;
                        }
                        break;

                    case 'h':
                        if (data[i] <= 1024) {
                            membershipValue = 0;
                        } else if (data[i] <= 2048) {
                            membershipValue = Math.abs((1024 - data[i]) / 1024);
                        } else if (data[i] <= 4096) {
                            membershipValue = Math.abs((4096 - data[i]) / 2048);
                        } else {
                            membershipValue = 0;
                        }
                        break;
                    case 'b':
                        if (data[i] <= 2048) {
                            membershipValue = 0;
                        } else if (data[i] <= 4096) {
                            membershipValue = Math.abs((2048 - data[i]) / 2048);
                        } else {
                            membershipValue = 1;
                        }
                        break;
                }
            } else if (i == 2) {
                switch (rule.charAt(i)) {
                    case 'n':
                        if (data[i] <= 0.01) {
                            membershipValue = 1;
                        } else if (data[i] < 0.02) {
                            membershipValue = Math.abs((0.02f - data[i]) / 0.01f);
                        } else {
                            membershipValue = 0;
                        }
                        break;

                    case 't':
                        if (data[i] <= 0.01) {
                            membershipValue = 0;
                        } else if (data[i] <= 0.02) {
                            membershipValue = Math.abs((0.01f - data[i]) / 0.01f);
                        } else if (data[i] < 0.05) {
                            membershipValue = Math.abs((0.05f - data[i]) / 0.03f);
                        } else {
                            membershipValue = 0;
                        }
                        break;

                    case 'v':
                        if (data[i] <= 0.02) {
                            membershipValue = 0;
                        } else if (data[i] <= 0.05) {
                            membershipValue = Math.abs((0.02f - data[i]) / 0.03f);
                        } else if (data[i] < 0.1) {
                            membershipValue = Math.abs((0.1f - data[i]) / 0.05f);
                        } else {
                            membershipValue = 0;
                        }
                        break;

                    case 'l':
                        if (data[i] <= 0.05) {
                            membershipValue = 0;
                        } else if (data[i] <= 0.1) {
                            membershipValue = Math.abs((0.05f - data[i]) / 0.05f);
                        } else if (data[i] < 0.15) {
                            membershipValue = Math.abs((0.15f - data[i]) / 0.05f);
                        } else {
                            membershipValue = 0;
                        }
                        break;

                    case 'm':
                        if (data[i] <= 0.1) {
                            membershipValue = 0;
                        } else if (data[i] <= 0.15) {
                            membershipValue = Math.abs((0.1f - data[i]) / 0.05f);
                        } else if (data[i] < 0.2) {
                            membershipValue = Math.abs((0.2f - data[i]) / 0.05f);
                        } else {
                            membershipValue = 0;
                        }
                        break;

                    case 'h':
                        if (data[i] <= 0.15) {
                            membershipValue = 0;
                        } else if (data[i] <= 0.2) {
                            membershipValue = Math.abs((0.15f - data[i]) / 0.05f);
                        } else if (data[i] < 0.3) {
                            membershipValue = Math.abs((0.3f - data[i]) / 0.1f);
                        } else {
                            membershipValue = 0;
                        }
                        break;

                    case 'e':
                        if (data[i] <= 0.2) {
                            membershipValue = 0;
                        } else if (data[i] < 0.3) {
                            membershipValue = Math.abs((0.2f - data[i]) / 0.1f);
                        } else {
                            membershipValue = 1;
                        }
                        break;
                }
            }
            if (doSetToOne) {
                System.out.println("value is out of bound.");
                continue;
            }

            result = result * membershipValue;
        }
        return result;
    }

    /**
     * get repeat count for each channel.
     * calculate with noise ant real bandwidth and message priority.
     *
     * @param data: data[0]:priority, data[1]:RBW, data[2]:noise
     * @return repeat cont
     */
    public static int getFuzzyValue(float[] data) {
        float value = 0;
        float num = 0;
        float deNum = 0;

        for (int i = 0; i < stateTable.length; i++) {
            float result = getMax(getRules(i), data);

            switch (getStateTable(i)) {
                case '1':
                    value = 1;
                    break;
                case '2':
                    value = 2;
                    break;
                case '3':
                    value = 3;
                    break;
                case '4':
                    value = 4;
                    break;
                case '5':
                    value = 5;
                    break;

            }
            num += result * value;
            deNum += result;
        }

        float repeatNum = (num / deNum);
        return Math.round(repeatNum);
    }
}
