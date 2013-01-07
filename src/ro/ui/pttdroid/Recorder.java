package ro.ui.pttdroid;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Random;

import ro.ui.pttdroid.codecs.Speex;
import ro.ui.pttdroid.settings.AudioSettings;
import ro.ui.pttdroid.settings.CommSettings;
import ro.ui.pttdroid.util.AudioParams;
import ro.ui.pttdroid.util.PhoneIPs;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

public class Recorder extends Thread {
	
	private final int SO_TIMEOUT = 0;
	
	private AudioRecord recorder;
	/*
	 * True if thread is running, false otherwise.
	 * This boolean is used for internal synchronization.
	 */
	private boolean isRunning = false;	
	/*
	 * True if thread is safely stopped.
	 * This boolean must be false in order to be able to start the thread.
	 * After changing it to true the thread is finished, without the ability to start it again.
	 */
	private boolean isFinishing = false;
	private boolean isTestmode = false;
	
	private DatagramSocket socket;
	private DatagramPacket packet;
	
	private short[] pcmFrame = new short[AudioParams.FRAME_SIZE + offsetInShorts];
	private byte[] encodedFrame;
	
	private short[][] pcmHello = {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 1, -2, 2, -3, 5, -5, 6, -6, 6, -4, 0, 6, -14, 24, -35, 49, -63, 74, -84, 84, -68, -69, -1000, -1494, 688, 1690, 1656, 1086, 1081, -954, -629, -693, -1639, -481, -1461, -2695, -2640, -883, -569, -4848, -9233, -8759, -3353, 2221, 605, -3255, -7242, -5903, -5802, -5460, -3075, -2039, -989, -3729, -3415, -4301, -1860, -1115, -3327, -2155, 499, 3054, 1493, -667, -1549, 45, 2883, 4279, 2127, 2490, 5632, 2623, -464, 107, 1292, 4538, 4501, 2100, -8, 81, 2292, 650, -3034, -2999, -452, 2731, 1591, -2655, -4102, -5701, -3224, 219, -373, -2824, -858, 1011, -1808, -2766, -1104, 104, -371, -1974, -4682, -2645, 2193, 1940, -760, -1665, -1396, -552, 491, -1135, -3355, -2371, -846, -772, -138, 3437, 4636, 1520, 1395, 2079, 2473, 4656, 7009, 3717, 535, 3940, 4834, 5559, 6796, 5379, 5380, 7595, 7404, 4849, 3572, 5055, 6376, 5918, 4489, 3054, 2207, 1357, -656, -3537},{-2882, -563, -968, -2666, -2885, -1778, -861, -1814, -3654, -3199, -2889, -3234, -306, -1925, -3902, 1070, 3064, -1611, -3188, 2187, 2275, 363, 602, -1625, -672, 4814, 2495, -2130, 399, 3522, 1739, -3102, -2206, -1269, 474, 1746, -604, -2424, -320, 1113, -2316, -2006, -92, 140, -1336, -3733, -4768, -3429, -1891, -2202, -4449, -5350, -1518, 1000, -2055, -3764, -3761, -4552, -1772, 805, -594, -1780, -1018, -538, -944, -29, 211, 37, 1372, 441, 576, 279, -1169, -983, 185, -354, -590, 2193, 2148, 955, 102, -513, 328, 2388, 1553, 589, 837, 1500, 1858, 1283, 661, 493, 1857, 1080, 439, -983, -1082, 888, 1912, 1078, -1075, -1044, 926, 1698, -748, -947, 775, -1130, -1138, 89, -1630, -765, 1960, 397, 176, 3994, 3546, 2031, 5005, 5978, 3277, 3230, 3358, 1941, 3148, 4023, 1798, 2335, 3734, 1715, -40, -164, -913, -1793, -1549, -1187, -972, -1173, -1502, -1723, -2412, -3574, -3900, -2962, -2745, -3753, -4421, -4156, -2805, -2672, -4451, -4004, -2610, -3945, -4708, -3947, -2524, -2693, -3401, -2264},{-1781, -3185, -2673, -1043, -2308, -2270, -1277, -2218, -2928, -1611, -2612, -3400, -1716, -2209, -4168, -3395, -1396, -1352, -640, 184, -1055, -2302, -1604, -824, 509, 1814, 2408, 3847, 4623, 4207, 4203, 4339, 3581, 2915, 3708, 4108, 3806, 2959, 1321, 1873, 3138, 1622, 1401, 2981, 2962, 2473, 3227, 2522, -20, -320, -367, -787, -709, -2021, -3918, -4492, -5026, -4971, -3040, -2656, -3343, -1861, -547, -918, -800, -1283, -1620, -276, 1423, 2741, 4304, 5401, 4303, 3363, 4098, 5545, 7019, 7030, 5980, 5873, 7021, 6885, 5489, 5139, 4266, 5241, 7125, 11212, 12457, 16743, 17019, -5280, -21693, -13958, -4642, -2381, 2550, 4084, 1622, 6249, 13556, 8083, -1903, -7408, -10221, -9607, -5715, -2629, -5195, -6504, -3451, -1324, -593, 586, -356, -3378, -2455, 1774, 4913, 8521, 10210, 6215, 5395, 11075, 13960, 12003, 10557, 8835, 5993, 7014, 7024, 5731, 8619, 11124, 176, -18186, -21992, -13309, -7737, -5096, -2673, -4043, -2189, 5026, 6851, -1249, -10136, -14833, -17544, -15043, -6074, -3240, -10159, -10573, -4951, -4621, -7641, -9419, -12411, -12104},{-5887, -3088, -3822, -888, 2604, 5088, 9375, 10193, 8309, 10856, 13286, 10065, 8017, 9224, 10193, 8522, 7755, 9727, 15407, 15539, -4620, -20752, -15436, -9004, -6674, -1592, -1447, -3830, 3768, 12912, 7612, -3069, -9300, -14248, -14456, -8624, -2586, -5056, -9773, -4914, 1446, -219, -7066, -11641, -10947, -7252, -3287, -1878, -1793, 1815, 8013, 12237, 12967, 12298, 11705, 11435, 11720, 11543, 10524, 10152, 9839, 9831, 9622, 12925, 18990, 1239, -25318, -24288, -13211, -12180, -7913, -3086, -6556, -2852, 11222, 13384, -1542, -9557, -14089, -18511, -13979, -7900, -8838, -12170, -7586, -2074, 366, 752, -8347, -17092, -13194, -5200, -1603, -2444, -6, 4163, 9561, 17468, 18012, 12562, 11419, 14533, 12835, 11846, 13462, 10513, 7940, 10926, 13423, 16355, 19604, -604, -24943, -19029, -8923, -10398, -6807, -5001, -9099, -1042, 13997, 12976, 498, -4902, -9579, -13121, -7529, -6133, -12180, -12019, -4843, -971, 1243, 3288, -2841, -10706, -9071, -2134, -1983, -4925, -1331, 2266, 5980, 15294, 17138, 9503, 9644, 13369, 10785, 10275, 11626, 8718, 6653, 7722, 9065, 10434, 14526, -627, -24609, -19506},{-11295, -14132, -10336, -7517, -11964, -7099, 7760, 10043, 3545, 2030, -1905, -6871, -3951, -1435, -8606, -13684, -9116, -9188, -8522, -1820, -2733, -8216, -7231, -1488, -384, 55, 2142, 920, 1612, 7769, 12752, 10157, 8336, 12276, 11835, 10858, 15120, 13742, 11004, 12201, 12155, 10485, 14868, 14795, -6737, -16044, -8105, -11702, -13834, -7499, -11061, -17690, -6507, 4072, 1538, 5274, 7692, 181, 1613, 7416, 4048, -2850, -4558, -5866, -11820, -10356, -7151, -11811, -14935, -11552, -7836, -6035, -1219, 1881, 1129, 4379, 10670, 10721, 8820, 10169, 9250, 7204, 9010, 9999, 7698, 8322, 9086, 8541, 8717, 13865, 8147, -8358, -7807, -6666, -12189, -11327, -11561, -17961, -19681, -9384, -3914, -4768, 1206, 2315, -1233, 4946, 9815, 5397, 1839, 553, -4957, -8135, -6290, -9601, -14893, -16661, -17005, -14063, -8163, -4397, -2299, 1351, 5930, 10925, 13842, 13587, 12992, 13498, 13084, 11889, 11764, 10327, 8975, 7475, 6828, 7248, 11455, 9515, -5965, -8140, -4725, -9095, -8177, -8601, -14566, -16512, -9578, -3763, -4266, -587, 1090, -2151, 3998, 10680, 8238, 5270, 2729, -3363, -5971, -3760},{-6739, -12301, -14745, -16907, -15296, -7937, -4237, -4104, -1006, 2938, 7494, 12876, 14275, 12025, 11778, 13434, 12906, 13059, 12819, 9296, 7524, 7732, 7731, 9276, 12898, 2558, -10493, -6708, -6834, -10084, -7963, -10635, -16233, -14469, -5896, -2811, -2982, 820, -370, -950, 6850, 10241, 7012, 4387, 846, -4330, -4166, -2341, -7349, -12008, -14330, -16200, -11102, -4449, -3980, -4076, -1221, 3165, 8666, 13427, 13126, 10689, 12016, 13361, 12501, 12957, 11597, 7698, 5957, 6463, 6032, 7859, 11354, -1460, -12513, -6934, -7419, -9267, -7111, -10720, -16431, -13678, -4204, -1199, -1484, 2324, 729, 585, 8771, 11019, 6484, 3562, -149, -4831, -3900, -3339, -9361, -14250, -15963, -17084, -12552, -5688, -5445, -5509, -1230, 3680, 8653, 12983, 13056, 10432, 11812, 13921, 12595, 12674, 10904, 6692, 5242, 5105, 5210, 6843, 10659, 164, -12008, -5951, -5504, -9317, -7084, -10329, -16972, -15598, -6279, -2685, -3482, 1237, 488, -2092, 5474, 10199, 5897, 3422, 1509, -4604, -5557, -2620, -6249, -13302, -15101, -15689, -15817, -9044, -4926, -7166, -5139, 1289, 5312, 9485, 13378, 11449, 9665, 13176},{14111, 11726, 11356, 9136, 5656, 5147, 7490, 8564, 11772, 8264, -6181, -7442, -2712, -6166, -6593, -6375, -12630, -16507, -9545, -1518, -2550, -1091, 2030, -2285, 696, 9824, 8551, 3665, 2660, -2798, -6727, -2555, -2607, -9765, -13509, -14131, -17171, -13046, -4785, -5799, -7068, -1015, 3858, 7315, 13407, 14235, 9856, 10514, 13721, 11994, 11022, 11141, 7316, 4255, 6607, 9035, 9489, 12457, 2199, -11250, -6660, -4287, -8965, -8400, -9434, -16126, -16547, -6319, -824, -3838, -514, 1108, -3366, 3080, 11141, 6681, 2477, 2280, -3734, -6670, -1926, -3476, -11913, -14536, -13530, -16014, -11519, -2516, -3703, -5452, 2180, 7709, 9173, 14196, 14969, 9413, 10126, 14927, 12676, 9409, 9715, 7574, 3783, 5433, 8391, 7817, 10623, 2791, -10344, -6667, -3016, -7197, -7393, -7823, -13750, -16092, -7479, 241, -2952, -1898, 2072, -2022, 1277, 11265, 9487, 3228, 3044, -1790, -7621, -3184, -1533, -10155, -14648, -12638, -14394, -13544, -4542, -1489, -5765, -1418, 6553, 7338, 10469, 15497, 11584, 7360, 11787, 14153, 10269, 9597, 10384, 6482, 4611, 10049, 11917, 11731, 11789, -683, -8212, -1949},{-2138, -6572, -7669, -11130, -16123, -15259, -6482, -1593, -5308, -3577, 339, -1964, 2922, 11114, 7577, 1817, -528, -3189, -4172, -4421, -6240, -11792, -16823, -15094, -12884, -11042, -5225, -3564, -5251, -510, 5361, 8008, 11435, 11926, 8723, 7703, 10035, 11316, 9711, 8077, 6564, 4493, 5155, 8554, 10305, 12719, 6613, -6743, -5862, -827, -4440, -5443, -5712, -11620, -15181, -10829, -2762, -771, -3234, -445, 514, -1359, 5466, 10772, 5666, 841, -2525, -5841, -5625, -5411, -8017, -12546, -15800, -14227, -11525, -8349, -2427, -805, -1653, 2500, 6075, 8443, 12259, 11562, 7884, 7868, 10124, 10548, 9548, 8560, 7267, 5429, 5881, 8696, 11414, 13410, 3299, -11632, -8564, -2181, -7833, -9010, -6620, -14007, -18469, -9425, 708, 1433, 476, 3687, 1955, -455, 8049, 12168, 2674, -3460, -6845, -13191, -11019, -5853, -10249, -14935, -13422, -11926, -8605, -942, 3364, 1261, 671, 4534, 6719, 8288, 12225, 10247, 2892, 3612, 9163, 9474, 9263, 10372, 7832, 6866, 11329, 14513, 18107, 14885, -8399, -22487, -11046, -10109, -16926, -8209, -9852, -22276, -9735, 9940, 9696, 10241, 13807, 3566},{-3072, 5233, 6342, -3996, -10457, -16308, -22216, -15277, -3280, -2160, -5526, -5001, -2870, 965, 5209, 5042, -69, -5428, -4965, -185, 1486, 2637, 6007, 5936, 3937, 8747, 14666, 14312, 13287, 12873, 9041, 7554, 11612, 11233, 12397, 10889, -10195, -26829, -15165, -8374, -12846, -3773, -2285, -11766, -1679, 14670, 15190, 13810, 10909, -275, -6315, -3448, -1956, -5476, -13353, -19753, -17717, -11022, -2884, 3248, -533, -7073, -4830, 543, 907, -141, -4286, -10899, -9942, -2061, 2099, 2790, 5062, 6657, 7853, 10547, 14700, 15510, 13608, 10809, 8598, 7768, 7781, 10599, 10055, 13576, 7234, -10360, -18793, -13140, -8630, -9554, -4388, -4574, -6854, -1911, 10466, 13144, 10834, 7133, 361, -6223, -5829, -3436, -7679, -12454, -18832, -17952, -11797, -3493, 142, -789, -4148, -4162, -125, 1597, 1920, -1846, -6494, -7730, -2640, 1245, 4069, 7181, 8198, 8466, 10180, 14468, 15029, 15571, 12090, 8738, 6814, 7488, 9276, 9930, 15510, 1023, -15159, -18419, -15814, -11971, -7418, -3487, -9641, -7945, -2191, 5194, 9262, 10524, 5318, -4228, -7370, -6900, -5985, -8075, -9332, -16476, -17133, -9417},{-1106, 2075, 2600, 1804, -1661, -436, 2111, 3339, -1678, -4592, -7060, -7136, -2368, 2988, 5199, 6501, 10386, 10788, 13071, 16091, 14907, 11594, 8383, 7195, 5445, 7337, 7736, 9705, 11770, -2958, -12485, -14544, -12663, -11693, -7081, -5025, -8865, -5189, -484, 5857, 7428, 9518, 4837, -832, -3603, -4393, -4978, -7781, -10918, -13561, -11860, -8124, -1219, 408, 1439, 601, 172, -399, 438, 317, -3986, -5374, -6621, -5484, -3379, 1369, 4041, 7365, 10330, 10998, 11755, 13216, 13261, 11794, 9975, 8147, 6601, 7349, 7627, 10609, 12850, -1745, -9677, -12966, -13553, -12004, -5248, -3229, -6865, -3010, -1984, 3020, 6332, 10601, 5229, -37, -4355, -7063, -7756, -10771, -10548, -14086, -13801, -13040, -7075, -4609, -1252, 891, 526, 2, -1206, 132, -2393, -1559, -4157, -4554, -4160, -416, 3076, 7163, 11790, 12521, 13765, 13292, 13553, 11838, 12427, 10156, 7666, 5878, 5673, 5902, 11332, 6071, -5640, -9962, -14935, -14685, -11576, -5162, -6927, -5172, -4695, -3453, 1329, 5815, 7981, 4377, 1427, -4739, -8007, -11308, -9606, -10477, -11008, -10954, -9833, -7224, -4010, 2090},{3054, 3551, 2099, 2129, -312, 515, 185, -2008, -2066, -1926, 312, 2523, 8155, 9785, 11891, 11999, 11870, 10326, 10130, 9679, 7670, 6563, 3457, 4132, 3434, 8403, 1598, -5737, -10168, -15532, -14524, -11659, -5978, -6837, -2839, -3991, -1921, 2354, 6383, 8935, 7074, 5455, -1691, -4585, -7287, -6846, -7470, -6947, -7488, -7438, -5228, -4134, -290, 1681, 3301, 1497, 1300, -1327, -2188, -1687, -2459, -2195, -1968, 279, 1264, 6006, 8472, 10651, 11721, 11811, 10557, 9679, 10047, 7642, 8011, 5598, 5921, 5231, 8808, 6904, -1304, -4764, -11279, -12235, -11322, -7498, -7830, -5126, -4048, -5006, -1070, 1194, 3672, 3602, 4087, -731, -2285, -4478, -6862, -7857, -8202, -8092, -8175, -5183, -5956, -3151, -1467, 396, 1237, 2245, 1690, 59, 734, -742, 574, 833, 2322, 3122, 5497, 6481, 7228, 9145, 8352, 8506, 7745, 7091, 5310, 5344, 4406, 2874, 4316, 4445, 5702, 6679, 1392, -4268, -8296, -12015, -12937, -10953, -9660, -8913, -6765, -6426, -4788, -1590, 585, 2038, 3745, 2305, -491, -2102, -4610, -6273, -6370, -6500, -6876, -5892, -5713, -4524},{-2620, -579, 1367, 3005, 3356, 2605, 3221, 3077, 3449, 3863, 3976, 3868, 4372, 4133, 3755, 4513, 4469, 4714, 4746, 4081, 3511, 3859, 3986, 4181, 5100, 5601, 6021, 7071, 6860, 3826, -328, -4092, -7887, -9466, -9162, -8260, -6233, -4311, -3475, -2888, -1398, -515, 1202, 2264, 2016, 1047, -771, -3069, -4806, -4792, -5623, -5578, -5449, -5523, -5293, -4510, -3261, -2276, -522, 137, 758, 854, 1260, 1666, 2107, 3061, 3011, 3078, 2594, 2597, 2314, 2354, 2084, 2363, 2846, 3036, 4378, 4788, 6198, 6187, 6162, 5613, 4865, 4430, 3105, 2404, 608, -549, -2407, -3188, -4415, -4861, -4561, -4848, -4105, -3914, -3517, -3935, -3207, -3368, -3049, -2704, -2724, -2712, -3409, -3508, -4241, -3791, -3757, -2906, -2480, -2049, -1428, -1132, -179, 631, 2037, 2398, 3007, 2762, 2511, 2135, 2008, 1923, 1409, 1585, 1231, 1440, 1576, 2436, 2827, 3271, 3210, 2775, 2714, 2309, 2365, 2243, 2475, 1948, 1962, 1670, 1099, 1001, 810, 746, 117, -399, -1026, -1721, -2184, -1809, -1398, -961, -466, -456, -453, -414, -757, -972},{-1119, -1775, -2016, -2343, -2696, -2545, -2433, -2777, -2477, -2059, -2031, -1975, -2187, -2243, -1843, -1382, -1324, -1065, -1027, -777, -341, -100, 113, 571, 946, 1068, 1235, 1019, 994, 1101, 1464, 2145, 2948, 3236, 3384, 3834, 3745, 3565, 3389, 2981, 2745, 2381, 1704, 794, -151, -493, -358, -128, -3, 505, 577, -9, -481, -728, -721, -344, 104, -376, -1162, -2058, -2704, -2846, -2649, -2551, -2115, -1867, -1993, -1512, -1352, -1074, -704, -483, -213, 565, 921, 1082, 1377, 1104, 1028, 811, 417, 170, 90, -67, 60, 168, 29, -99, -278, -452, -90, 656, 932, 884, 602, 192, -207, -436, -625, -377, -5, 4, 4, -139, -111, 53, 403, 869, 1265, 1406, 1008, 590, 342, 231, 249, 349, 361, 305, 114, -89, -486, -843, -557, -42, 598, 997, 1230, 1183, 1472, 1886, 2206, 2051, 1153, 416, -243, -381, -648, -669, -1162, -1561, -1748, -2091, -2031, -2315, -2006, -1868, -1895, -2009, -2039, -1852, -1744, -1648, -1710, -1756, -1638, -1339, -1326, -981, -341, -56},{235, 414, 495, 1032, 786, 523, 649, 286, 51, 3, -23, -95, 413, 394, 126, -468, -1084, -1073, -1070, -587, -425, -411, -509, -467, -416, -289, 205, 527, 704, 960, 963, 745, 588, 710, 1094, 709, 605, 544, 193, 57, 446, 672, 135, -154, -706, -1175, -1482, -1478, -1577, -1691, -1721, -1915, -1659, -1865, -1562, -1119, -976, -639, -469, -372, -821, -956, -507, -37, 250, 475, 526, 423, 797, 1262, 1287, 1407, 1644, 1808, 1963, 1853, 1730, 1709, 1584, 1367, 1184, 898, 523, 192, 32, 326, 214, -203, -118, -295, -192, 362, 615, 677, 413, 385, 783, 870, 719, 323, -128, -184, 75, 311, 84, 122, 289, 281, 271, -229, -394, -610, -745, -148, 296, 359, 64, -301, -473, -476, -355, -49, 220, 335, 34, 17, 417, 545, 214, -279, -105, 29, -5, -125, -119, -287, -588, -875, -1407, -1389, -1084, -763, -671, -820, -621, -162, -154, -187, 80, 689, 1092, 943, 533, 70, 137, -4, -157, -167, -477, -646},{-510, -514, -314, -80, -442, -282, 319, 394, 249, 290, -25, -72, 136, -24, 32, 36, -26, -236, -519, -273, -75, -508, -644, -528, -657, -707, -644, -864, -917, -434, -417, -354, 33, 34, -62, -184, -199, -95, -39, 237, 238, 109, -59, -446, -466, -329, -280, -388, -236, 259, 678, 1045, 1104, 892, 870, 888, 811, 1073, 1379, 1423, 1241, 812, 550, 599, 748, 944, 1090, 1133, 1079, 847, 440, 103, 105, 22, -271, -402, -499, -622, -523, -492, -722, -742, -430, -256, -256, -214, -240, -328, -634, -915, -1029, -1141, -1354, -1474, -1561, -1677, -1522, -1323, -993, -913, -874, -472, 62, 581, 961, 1193, 1309, 1316, 1334, 1523, 1494, 1573, 1648, 1727, 1448, 763, 343, 16, -81, -337, -458, -390, -411, -341, -321, -469, -333, -57, -54, 23, -33, -160, -343, -542, -657, -590, -404, -337, -183, -139, -195, -103, -198, -334, -294, -183, -124, -265, -236, -341, -320, 37, 175, 144, -164, -354, -317, -299, 6, 310},{287, 254, 149, 217, 251, 311, 262, -151, -213, -295, -349, -336, -317, -347, -329, -291, -388, -513, -717, -708, -655, -670, -906, -854, -573, -525, -457, -454, -193, 90, 189, 494, 732, 656, 511, 392, 296, 449, 638, 616, 450, 391, 472, 586, 704, 647, 475, 406, 605, 598, 363, 62, -8, 167, 205, 162, -64, -52, 158, 211, -14, -310, -673, -947, -884, -722, -462, -352, -387, -552, -438, -504, -751, -588, -425, -108, 34, 116, 232, 72, 140, 370, 285, 256, 403, 513, 630, 794, 723, 764, 1036, 1087, 1187, 990, 835, 890, 1031, 1173, 1118, 944, 606, 530, 467, 485, 441, 148, 114, -119, -528, -546, -620, -836, -895, -786, -649, -609, -396, -300, -250, -381, -727, -945, -1112, -1078, -1097, -1273, -1372, -974, -530, -399, -285, -97, 205, 480, 334, -78, -313, -480, -611, -758, -742, -631, -384, -139, 39, 130, 135, 142, 102, 216, 193, 243, 114, 29, 164, -53, -275, -253, -111, -348}};
	/*
	 * Frame sequence number.  Reset to 1 between transmissions.
	 */
	private int seqNum;
	
	/*
	 * Offset in bytes for leaving room for a header containing the seqNum.
	 */
	public static int offsetInBytes = 4;
	public static int offsetInShorts = offsetInBytes/2;
	
	/*
	 * Set to true FOR TESTING PURPOSES ONLY
	 */
	private static boolean introduceFakeLosses = false;
	
	private int hello_locator = 0;

	public void run() {
		// Set audio specific thread priority
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		
		while(!isFinishing()) {		
			init();
			while(isRunning()) {
				
				try {		
					// if Testmode is ON, use the pcmHello frame, encode and send it
					if (isTestmode) {
						while (isTestmode) {
							this.playSingleHello();
							}
					} else {
						ByteBuffer target = ByteBuffer.wrap(encodedFrame);
						byte[] headerBytes = ByteBuffer.allocate(offsetInBytes).putInt(seqNum).array();
						
						// Read PCM from the microphone buffer & encode it
						if(AudioSettings.useSpeex()==AudioSettings.USE_SPEEX) {
							// read data into pcmFrame
							int readStatus = recorder.read(pcmFrame, 0, AudioParams.FRAME_SIZE);
							
							Log.i ("Read status: ", String.format("%d", readStatus));
							
							// encode audio in pcmFrame into encodedFrame to be sent in datagram
							byte[] audioData = new byte[Speex.getEncodedSize(AudioSettings.getSpeexQuality())];
							Speex.encode(pcmFrame, audioData);	
							
							target.put(headerBytes);
							target.put(audioData);
						}
						else {
							target.put(headerBytes);
							recorder.read(encodedFrame, offsetInBytes, AudioParams.FRAME_SIZE_IN_BYTES);						
						}
						
						if (introduceFakeLosses) {
							Random randomGeneator = new Random();
							// this should be 25 percent loss
							if (randomGeneator.nextDouble() > 0.75) {
								seqNum += 2; // introduce fake loss
							} else {
								seqNum++;
							}
						} else {
						    seqNum++;
						}
						// Send encoded frame packed within an UDP datagram
						socket.send(packet);
					}
				}
				catch(IOException e) {
					Log.d("Recorder", e.toString());
				}	
			}		
		
			release();	
			/*
			 * While is not running block the thread.
			 * By doing it, CPU time is saved.
			 */
			synchronized(this) {
				try {	
					if(!isFinishing())
						this.wait();
				}
				catch(InterruptedException e) {
					Log.d("Recorder", e.toString());
				}
			}					
		}							
	}
	
	private void init() {				
		try {	    	
			seqNum = 1;
			
			PhoneIPs.load();
			
			socket = new DatagramSocket();
			socket.setSoTimeout(SO_TIMEOUT);
			InetAddress addr = null;
			
			switch(CommSettings.getCastType()) {
			case CommSettings.BROADCAST:
				socket.setBroadcast(true);		
				addr = CommSettings.getBroadcastAddr();
				break;
			case CommSettings.MULTICAST:
				addr = CommSettings.getMulticastAddr();					
				break;
			case CommSettings.UNICAST:
				addr = CommSettings.getUnicastAddr();					
				break;
			}							
			
			// will use 4 bytes for storing seq number
			if(AudioSettings.useSpeex()==AudioSettings.USE_SPEEX) {
				encodedFrame = new byte[Speex.getEncodedSize(AudioSettings.getSpeexQuality()) + offsetInBytes];
			}
			else { 
				encodedFrame = new byte[AudioParams.FRAME_SIZE_IN_BYTES  + offsetInBytes];
			}
			
			packet = new DatagramPacket(
					encodedFrame, 
					encodedFrame.length, 
					addr, 
					CommSettings.getPort());

	    	recorder = new AudioRecord(
	    			AudioSource.MIC, 
	    			AudioParams.SAMPLE_RATE, 
	    			AudioFormat.CHANNEL_CONFIGURATION_MONO, 
	    			AudioParams.ENCODING_PCM_NUM_BITS, 
	    			AudioParams.RECORD_BUFFER_SIZE);
	    	
			recorder.startRecording();				
		}
		catch(SocketException e) {
			Log.d("Recorder", e.toString());
		}	
	}
	
	public void playSingleHello() {
		while (hello_locator < 15 ) {
			ByteBuffer target = ByteBuffer.wrap(encodedFrame);
			byte[] headerBytes = ByteBuffer.allocate(offsetInBytes).putInt(seqNum).array();

			System.arraycopy(pcmHello[hello_locator++], 0, pcmFrame, 0, AudioParams.FRAME_SIZE);
			// encode audio in pcmFrame into encodedFrame to be sent in datagram
			byte[] audioData = new byte[Speex.getEncodedSize(AudioSettings.getSpeexQuality())];
			Speex.encode(pcmFrame, audioData);	
			//Speex.encode(pcmFrame, encodedFrame);

			target.put(headerBytes);
			target.put(audioData);

			if (introduceFakeLosses) {
				Random randomGeneator = new Random();
				// this should be 25 percent loss
				if (randomGeneator.nextDouble() > 0.75) {
					seqNum += 2; // introduce fake loss
				} else {
					seqNum++;
				}
			} else {
			    seqNum++;
			}

			// Send encoded frame packed within an UDP datagram
			try {
				socket.send(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // while loop for playing a single hello
		hello_locator = 0;
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void release() {			
		if(recorder!=null) {
			recorder.stop();
			recorder.release();
		}
	}
	
	public synchronized boolean isRunning() {
		return isRunning;
	}
	
	public synchronized void resumeAudio() {				
		isRunning = true;
		this.notify();
	}
		
	public synchronized void pauseAudio() {				
		isRunning = false;	
		socket.close();
	}	 
		
	public synchronized boolean isFinishing() {
		return isFinishing;
	}
	
	public synchronized void finish() {
		pauseAudio();
		isFinishing = true;		
		this.notify();
	}

	public synchronized boolean getTestmode() {
		return isTestmode;
	}

	public synchronized void setTestmode(boolean isTestmode) {
		this.isTestmode = isTestmode;
	}
	
}
