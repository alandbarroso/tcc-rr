package agent;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.Entity;

import utilities.Logger;

public class Radar {
	// Moshtarak bein noCommi o commi
	public static final int HEADER_HELPUNHELP = 0;
	//offset: HELP =====>1
	//offset: UNHELP ====>0
	public static final int HEADER_CIVILIAN = 1;
	public static final int HEADER_BUILDING_OUT_OF_REACH_NEAR_ROADS = 2;
	public static final int HEADER_AMBHELP = 3;
	public static final int HEADER_AGEPOSITION= 7;
	public static final int HEADER_WAY = 5;

	// Serf Commi
	public static final int HEADER_CENTER = 6;
	// offset:0 ====>POLICEFORCE
	// offset;1 ====>AMBULANCEAGENT
	//public static final int HEADER_REQUEST = 7;
	public static final int HEADER_BLOCKADE = 8;
	public static final int HEADER_CLEAR = 9;
	public static final int HEADER_NOWAY = 10;
	public static final int HEADER_AMBUNHELP = 11;
	public static final int HEADER_EMPTYBUILDINGS = 12;
	public static final int HEADER_BUILDING = 13;
	public static final int HEADER_AMB = 14;
	public static final int HEADER_SAYMYPOS = 15;
	public static final int HEADER_FIRECENTER = 4;

	public Logger logger = null;
	public int powNum;
	public static int tedadBitFireness = 3;
	public int lengthOfCvID = 34;

	public Radar(int powNum) {
		this.powNum = powNum;
	}

	public int civilianPowNumForAmb = findPow(8);
	public static final int headerSize = 4;
	public ArrayList<Integer> sentBlockades = null;
	public int blockadeID = 0;

	public int maxGroupMembersSize = 8;
	public BitSet bitSet = new BitSet();
	public int HPZarib = 50;
	public int buriednessZarib = 1;
	public int damageZarib = 25;

	public static int findPow(int tedad) {
		int i = 0;
		int pow = 1;
		while (tedad > pow) {
			pow = pow * 2;
			i++;
		}
		return i;
	}

	public byte[] code(int header, int theOther) {
		bitSet = new BitSet();
		int start = 0;
		start = calcWithBitSet(bitSet, header, start, headerSize);
		start = calcWithBitSet(bitSet, theOther, start, powNum);
		byte[] byteArray = toByteArray(bitSet);
		return byteArray;
	}
	
	public byte[] codePlusOffset(int header, int theOther , int offset) {
		bitSet = new BitSet();
		int start = 0;
		start = calcWithBitSet(bitSet, header, start, headerSize);
		start = calcWithBitSet(bitSet, offset, start, 1);
		start = calcWithBitSet(bitSet, theOther, start, powNum);
		byte[] byteArray = toByteArray(bitSet);
		return byteArray;
	}

	public byte[] codeForClearer(int theOther, worldGraph.Area area) {
		bitSet = new BitSet();
		int start = 0;
		start = calcWithBitSet(bitSet, HEADER_CLEAR, start, headerSize);
		start = calcWithBitSet(bitSet, theOther, start, powNum);
		for (int i = 0; i < area.enterances.size(); i++)
			bitSet.set(i + start,
					area.enterances.get(i).isItConnectedToNeighbour);
		start = area.enterances.size() + start;
		bitSet.set(start);
		byte[] byteArray = toByteArray(bitSet);
		return byteArray;
	}

	public byte[] codeForAmbHelp(int header, int pos, int deadTime, int BN) {
		bitSet = new BitSet();
		int start = 0;
		start = calcWithBitSet(bitSet, header, start, headerSize);
		start = calcWithBitSet(bitSet, pos, start, powNum);
		start = calcWithBitSet(bitSet, deadTime, start, 9);
		start = calcWithBitSet(bitSet, BN, start, 7);
		byte[] byteArray = toByteArray(bitSet);
		return byteArray;
	}

	public BitSet mapHeader(int header) {
		BitSet bits = new BitSet();
		calcWithBitSet(bits, header, 0, headerSize);
		return bits;
	}

	public int mapCodeForAmb(BitSet bits, int myID, int theOther, int start,
			int areaPow) {
		start = calcWithBitSet(bits, myID, start, areaPow);
		start = calcWithBitSet(bits, theOther, start, civilianPowNumForAmb);
		return start;
	}

	public int mapCodeNum(BitSet bits, int num, int id, int start, int policePow) {
		start = calcWithBitSet(bits, id, start, policePow);
		start = calcWithBitSet(bits, num - 1, start,
				findPow(maxGroupMembersSize));
		return start;
	}

	public int CVlCodeForID(BitSet bits, int num, int start) {
		start = calcWithBitSet(bits, num, start, lengthOfCvID);
		return start;
	}

	public int CVlCodeForPos(BitSet bits, int num, int start, int powNumber) {
		start = calcWithBitSet(bits, num, start, powNumber);
		return start;
	}

	public int CVlCodeForDeadTimeAndBN(BitSet bits, int deadTime, int BN,
			int start) {
		start = calcWithBitSet(bits, deadTime, start, 9);
		start = calcWithBitSet(bits, BN, start, 7);
		return start;
	}

	public int buildingCode(BitSet bits, int id, int fireness, int lts,
			int powNumForLTS, int start) {
		start = calcWithBitSet(bits, id, start, powNum);
		start = calcWithBitSet(bits, fireness, start, tedadBitFireness);
		start = calcWithBitSet(bits, lts, start, powNumForLTS);
		return start;
	}

	public static byte[] codeToByteArray(BitSet bits) {
		byte[] byteArray = toByteArray(bits);
		return byteArray;
	}

	public int tellHeader(byte[] bit) {
		BitSet bits = new BitSet();
		bits = fromByteArray(bit);
		int header = fromMabnaye2ToMabnaye10(bits, headerSize, 0);
		return header;
	}

	public boolean tellHeaderForPC(byte[] bit) {
		BitSet bits = new BitSet();
		bits = fromByteArray(bit);
		if (fromMabnaye2ToMabnaye10(bits, headerSize + 1, headerSize) == 1)
			return false;
		else
			return true;
	}

	public int decode(byte[] bit) {
		BitSet bits = new BitSet();
		bits = fromByteArray(bit);
		int pos = fromMabnaye2ToMabnaye10(bits, headerSize + powNum, headerSize);
		return pos;
	}

	public ArrayList<Integer> decodeForAmbHelp(byte[] bit) {
		BitSet bits = new BitSet();
		bits = fromByteArray(bit);
		ArrayList<Integer> IDP = new ArrayList<Integer>();
		IDP.add(fromMabnaye2ToMabnaye10(bits, headerSize + powNum, headerSize));
		IDP.add(fromMabnaye2ToMabnaye10(bits, 9 + powNum + headerSize,
				headerSize + powNum));
		IDP.add(fromMabnaye2ToMabnaye10(bits, 9 + powNum + headerSize + 7,
				headerSize + powNum + 9));
		return IDP;
	}

	public int[] decodeForClearer(byte[] bit, StandardWorldModel model,
			HashMap<Integer, Entity> allRanks) {
		BitSet bits = new BitSet();
		bits = fromByteArray(bit);
		int lastBitPos = -1;
		for (lastBitPos = bit.length * 8 - 1; lastBitPos > 0; lastBitPos--)
			if (bits.get(lastBitPos) == true)
				break;
		bits.set(lastBitPos, false);
		int a = powNum + headerSize;
		Entity entit = allRanks
				.get(fromMabnaye2ToMabnaye10(bits, a, headerSize));
		int pos = entit.getID().getValue();
		worldGraph.Area area = ((Area) entit).worldGraphArea;
		int[] array = new int[area.enterances.size() + 1];
		array[0] = pos;
		for (int i = a; i < area.enterances.size() + a; i++) {
			if (bits.get(i) == true)
				array[i - a + 1] = 1;
			else
				array[i - a + 1] = 0;
		}
		return array;
	}

	public ArrayList<Integer> CVlDecode(byte[] bit, StandardWorldModel model,
			HashMap<Integer, Entity> allRanks, boolean noCommi) {
		ArrayList<Integer> IDP = new ArrayList<Integer>();
		BitSet bits = new BitSet();
		bits = fromByteArray(bit);
		int lastBitPos = -1;
		for (lastBitPos = bit.length * 8 - 1; lastBitPos > 0; lastBitPos--)
			if (bits.get(lastBitPos) == true)
				break;
		if (!noCommi) {
			int i = powNum + headerSize;
			Area a = (Area) allRanks.get(fromMabnaye2ToMabnaye10(bits,
					headerSize + powNum, headerSize));
			IDP.add(a.getID().getValue());
			int powNumber = findPow(a.nearBuildings.size() + 1);
			while (i < lastBitPos) {
				IDP.add(fromMabnaye2ToMabnaye10(bits, i + lengthOfCvID, i));
				int b = fromMabnaye2ToMabnaye10(bits, i + lengthOfCvID + powNumber,
						i + lengthOfCvID);
				if (b == 0)
					IDP.add(IDP.get(0));
				else {
					int ab = fromMabnaye2ToMabnaye10(bits, i + lengthOfCvID + powNumber,
							i + lengthOfCvID);
					ab = ab - 1;
					ArrayList<Building> nearBuildings = a.nearBuildings;
					Building build = nearBuildings.get(ab);
					int cd = build.getID().getValue();
					IDP.add(cd);
				}
				IDP.add(fromMabnaye2ToMabnaye10(bits, i + lengthOfCvID + powNumber + 9, i
						+ lengthOfCvID + powNumber));
				IDP.add(fromMabnaye2ToMabnaye10(bits, i + lengthOfCvID+ powNumber + 9
						+ 7, i + lengthOfCvID + powNumber + 9));
				i = i + lengthOfCvID + powNumber + 9 + 7;
			}
		} else {
			int i = headerSize;
			IDP.add(fromMabnaye2ToMabnaye10(bits, i, 0));
			while (i < lastBitPos) {
				IDP.add(fromMabnaye2ToMabnaye10(bits, i + lengthOfCvID, i));
				IDP.add(allRanks
						.get(fromMabnaye2ToMabnaye10(bits, i + lengthOfCvID + powNum,
								i + lengthOfCvID)).getID().getValue());
				IDP.add(fromMabnaye2ToMabnaye10(bits, i + lengthOfCvID + powNum + 9, i
						+ lengthOfCvID + powNum));
				IDP.add(fromMabnaye2ToMabnaye10(bits, i + lengthOfCvID + powNum + 9 + 7,
						i + lengthOfCvID + powNum + 9));
				i = i + lengthOfCvID + powNum + 9 + 7;
			}
		}
		return IDP;
	}

	public ArrayList<Integer> BuildingDecode(byte[] bit,
			StandardWorldModel model, HashMap<Integer, Entity> allRanks,
			int time, boolean lowCommi) {
		ArrayList<Integer> IDP = new ArrayList<Integer>();
		BitSet bits = new BitSet();
		bits = fromByteArray(bit);
		int lastBitPos = -1;
		for (lastBitPos = bit.length * 8 - 1; lastBitPos > 0; lastBitPos--)
			if (bits.get(lastBitPos) == true)
				break;
		bits.set(lastBitPos, false);
		int i = headerSize;
		int a = fromMabnaye2ToMabnaye10(bits, i + powNum, i);
		Area area = (Area) allRanks.get(a);
		IDP.add(area.getID().getValue());
		i = i + powNum;
		int powNumNearBuildings = findPow(area.nearBuildings.size() + 1);
		while (i < lastBitPos) {
			int b = fromMabnaye2ToMabnaye10(bits, i + powNumNearBuildings, i);
			if (b == 0)
				IDP.add(area.getID().getValue());
			else
				IDP.add(area.nearBuildings.get(b - 1).getID().getValue());
			IDP.add(fromMabnaye2ToMabnaye10(bits, i + tedadBitFireness
					+ powNumNearBuildings, i + powNumNearBuildings));
			if (lowCommi) {
				IDP.add(time
						- fromMabnaye2ToMabnaye10(bits, i + tedadBitFireness
								+ powNumNearBuildings + 3, i + tedadBitFireness
								+ powNumNearBuildings) - 1);
				i = i + tedadBitFireness + powNumNearBuildings + 3;
			} else {
				IDP.add(time
						- fromMabnaye2ToMabnaye10(bits, i + tedadBitFireness
								+ powNumNearBuildings + 1, i + tedadBitFireness
								+ powNumNearBuildings) - 1);
				i = i + tedadBitFireness + powNumNearBuildings + 1;
			}
		}
		return IDP;
	}

	public ArrayList<Integer> BuildingDecodeOutOfReach(byte[] bit, int time,
			boolean lowCommi, boolean noCommi) {
		ArrayList<Integer> IDP = new ArrayList<Integer>();
		BitSet bits = new BitSet();
		bits = fromByteArray(bit);
		int lastBitPos = -1;
		for (lastBitPos = bit.length * 8 - 1; lastBitPos > 0; lastBitPos--)
			if (bits.get(lastBitPos) == true)
				break;
		bits.set(lastBitPos, false);
		IDP.add(fromMabnaye2ToMabnaye10(bits, headerSize, 0));
		int i = headerSize;
		while (i < lastBitPos) {
			IDP.add(fromMabnaye2ToMabnaye10(bits, i + powNum, i));
			IDP.add(fromMabnaye2ToMabnaye10(bits,
					i + tedadBitFireness + powNum, i + powNum));
			if (noCommi) {
				IDP.add(fromMabnaye2ToMabnaye10(bits, i + tedadBitFireness
						+ powNum + 9, i + tedadBitFireness + powNum));
				i = i + tedadBitFireness + powNum + 9;
			} else if (lowCommi) {
				IDP.add(time
						- fromMabnaye2ToMabnaye10(bits, i + tedadBitFireness
								+ powNum + 3, i + tedadBitFireness + powNum)
						- 1);
				i = i + tedadBitFireness + powNum + 3;
			} else {
				IDP.add(time
						- fromMabnaye2ToMabnaye10(bits, i + tedadBitFireness
								+ powNum + 1, i + tedadBitFireness + powNum)
						- 1);
				i = i + tedadBitFireness + powNum + 1;
			}
		}
		return IDP;
	}

	protected ArrayList<Integer> decodeForEmptyBuildings(byte[] bit, int SBNum) {
		ArrayList<Integer> builds = new ArrayList<Integer>();
		BitSet bits = new BitSet();
		bits = fromByteArray(bit);
		int lastBitPos = -1;
		for (lastBitPos = bit.length * 8 - 1; lastBitPos > 0; lastBitPos--)
			if (bits.get(lastBitPos) == true)
				break;
		bits.set(lastBitPos, false);
		int i = headerSize;
		while (i < lastBitPos) {
			builds.add(fromMabnaye2ToMabnaye10(bits, i + SBNum, i));
			i = i + SBNum;
		}
		return builds;
	}

	protected void log(String msg) {
		// logger.log(msg);
	}

	public ArrayList<Integer> mapDecodeForPolice(byte[] bit, int policePow) {
		BitSet bits = new BitSet();
		bits = fromByteArray(bit);
		int lastBitPos = -1;
		for (lastBitPos = bit.length * 8 - 1; lastBitPos > 0; lastBitPos--)
			if (bits.get(lastBitPos) == true)
				break;
		bits.set(lastBitPos, false);
		ArrayList<Integer> IDP = new ArrayList<Integer>();
		int i = headerSize;
		while (i < lastBitPos) {
			IDP.add((fromMabnaye2ToMabnaye10(bits, i + policePow, i)) - 1);
			i += policePow;
			// int id = adadPoliceHa.get(fromMabnaye2ToMabnaye10(bits, i
			// + policePow, i));
			// i += policePow;
			// int a = fromMabnaye2ToMabnaye10(bits, i
			// + findPow(maxGroupMembersSize), i) + 1;
			// i += findPow(maxGroupMembersSize);
			// ArrayList<Integer> members = new ArrayList<Integer>();
			// for (int j = 0; j < a; j++) {
			// members.add(allRanks.get(fromMabnaye2ToMabnaye10(bits, i
			// + powNum, i)));
			// i += powNum;
			// }
			// map.put(id, members);
		}
		return IDP;
	}

	public ArrayList<Integer> mapDecodeForAmb(byte[] bit, int areaPow) {
		ArrayList<Integer> IDP = new ArrayList<Integer>();
		BitSet bits = new BitSet();
		bits = fromByteArray(bit);
		int lastBitPos = -1;
		for (lastBitPos = bit.length * 8 - 1; lastBitPos > 0; lastBitPos--)
			if (bits.get(lastBitPos) == true)
				break;
		bits.set(lastBitPos, false);
		int i = headerSize;
		while (i < lastBitPos) {
			int a = fromMabnaye2ToMabnaye10(bits, i + areaPow, i);
			if (a == 0)
				IDP.add(-1);
			else
				IDP.add(a - 1);
			IDP.add(fromMabnaye2ToMabnaye10(bits, i + areaPow
					+ civilianPowNumForAmb, i + areaPow));
			i = i + areaPow + civilianPowNumForAmb;
		}
		return IDP;
	}

	public static BitSet fromByteArray(byte[] bytes) {
		BitSet bits = new BitSet();
		for (int i = 0; i < bytes.length * 8; i++) {
			if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
				bits.set(i);
			}
		}
		return bits;
	}

	public static byte[] toByteArray(BitSet bits) {
		byte[] bytes = new byte[bits.length() / 8 + 1];
		for (int i = 0; i < bits.length(); i++) {
			if (bits.get(i)) {
				bytes[bytes.length - i / 8 - 1] |= 1 << (i % 8);
			}
		}
		return bytes;
	}

	public static ArrayList<Integer> toMabnaye2(int num) {
		ArrayList<Integer> baghiMandeHa = new ArrayList<Integer>();
		ArrayList<Integer> barMabnaye2 = new ArrayList<Integer>();
		int maghsom = num;
		while (maghsom >= 2) {
			baghiMandeHa.add(maghsom % 2);
			maghsom = maghsom / 2;
		}
		baghiMandeHa.add(maghsom);
		for (int i = baghiMandeHa.size() - 1; i >= 0; i--) {
			barMabnaye2.add(baghiMandeHa.get(i));
		}
		return barMabnaye2;
	}

	public static int fromMabnaye2ToMabnaye10(BitSet bits, int end, int start) {
		int num = 0;
		for (int i = start; i < end; i++)
			if (bits.get(i) == true) {
				num = num + (int) Math.pow(2, end - i - 1);
			}
		return num;
	}

	public static int calcWithBitSet(BitSet bits, int num, int start,
			int powNumber) {
		int end = start + powNumber;
		if(end == start)
			return start;
		ArrayList<Integer> barMabnaye2 = toMabnaye2(num);
		int tafazol = powNumber - barMabnaye2.size();
		// merge
		for (int i = 0; i < barMabnaye2.size(); i++)
			if (barMabnaye2.get(i) == 1)
				bits.set(i + start + tafazol, true);
			else
				bits.set(i + start + tafazol, false);
		return end;
	}

	public int calcForBlockade(int[][] array,
			boolean[] isItUsefulToSendBlockade, BitSet bits, int start,
			int num, int areaPow) {
		mark(array);
		int home = 0;
		BitSet jaSet = new BitSet();
		for (int a = 0; a < array.length; a++)
			for (int i = a + 1; i < array.length; i++)
				if (array[a][i] != 2) {
					if (array[a][i] == 1)
						jaSet.set(home);
					home++;
				}
		for (int a = 0; a < array.length; a++) {
			if (array[a][a] == 1)
				jaSet.set(home);
			home++;
		}

		int tedad1Ha = 0;
		for (int a = 0; a < home; a++)
			if (jaSet.get(a) == true)
				tedad1Ha++;

		if (tedad1Ha == home) {
			return start;
		} else {
			if (tedad1Ha == 0) {
				isItUsefulToSendBlockade[0] = false;
				return start;
			} else {
				isItUsefulToSendBlockade[0] = true;
				start = calcWithBitSet(bits, num, start, areaPow);
				for (int i = 0; i < home; i++)
					if (jaSet.get(i) == true)
						bits.set(start + i);
			}
			start += home;
		}
		return start;
	}

	public ArrayList<Integer> decodeIDForNoWay(Agent agent, Entity IDOn,
			byte[] byteArray, StandardWorldModel model,
			HashMap<Integer, Entity> allRanks) {
		ArrayList<Integer> IDs = new ArrayList<Integer>();
		int lastBitPos = -1;
		BitSet bits = Radar.fromByteArray(byteArray);
		for (lastBitPos = byteArray.length * 8 - 1; lastBitPos > 0; lastBitPos--)
			if (bits.get(lastBitPos) == true)
				break;
		bits.set(lastBitPos, false);
		Entity entit = allRanks.get(fromMabnaye2ToMabnaye10(bits, headerSize
				+ powNum + 1, headerSize + 1));
		int ID = entit.getID().getValue();
		agent.addAgentPositionToModel(IDOn, (Area) entit);
		ArrayList<Road> nearRoads = ((Area) entit).nearRoads;
		int a = findPow(nearRoads.size() + 1);
		int i = headerSize + powNum + 1;
		IDs.add(ID);
		while (i < lastBitPos) {
			int index = fromMabnaye2ToMabnaye10(bits, i + a, i);
			if (index == 0)
				IDs.add(ID);
			else
				IDs.add(nearRoads.get(index - 1).getID().getValue());
			i += a;
		}
		return IDs;
	}

	public ArrayList<Integer> decodeIDForNoWayAD(byte[] byteArray,
			StandardWorldModel model, HashMap<Integer, Entity> allRanks) {
		ArrayList<Integer> IDs = new ArrayList<Integer>();
		int lastBitPos = -1;
		BitSet bits = Radar.fromByteArray(byteArray);
		for (lastBitPos = byteArray.length * 8 - 1; lastBitPos > 0; lastBitPos--)
			if (bits.get(lastBitPos) == true)
				break;
		bits.set(lastBitPos, false);
		int i = headerSize + 1;
		while (i < lastBitPos) {
			IDs.add(allRanks.get(fromMabnaye2ToMabnaye10(bits, i + powNum, i))
					.getID().getValue());
			i += powNum;
		}
		return IDs;
	}

	public int decodeIDForBlockade(byte[] byteArray) {
		BitSet bits = fromByteArray(byteArray);
		blockadeID = fromMabnaye2ToMabnaye10(bits, powNum + headerSize + 1,
				headerSize + 1);
		return blockadeID;
	}

	public int shoro = 0;

	public int[][] unCalcForBlockade(BitSet bits, Area area, int start,
			int pos, Logger logger, StandardWorldModel model) {
		ArrayList<Road> nearRoads = area.nearRoads;
		int tedadNearRoads = nearRoads.size();
		int ID = fromMabnaye2ToMabnaye10(bits, start
				+ findPow(tedadNearRoads + 1), start);
		if (ID == 0)
			ID = pos;
		else
			ID = nearRoads.get(ID - 1).getID().getValue();
		worldGraph.Area _area = ((Area) model.getEntityByInt(ID)).worldGraphArea;
		int entrancesNum = _area.enterances.size();
		int[][] array = new int[entrancesNum][entrancesNum];
		for (int i = 0; i < array.length; i++)
			for (int j = i; j < array.length; j++)
				array[i][j] = 2;
		int home = start + findPow(tedadNearRoads + 1);
		for (int j = 0; j < entrancesNum; j++) {
			for (int i = j + 1; i < array.length; i++)
				if (array[j][i] == 2) {
					if (bits.get(home) == true)
						array[j][i] = 1;
					else
						array[j][i] = 0;
					home++;
				}
			for (int a = j + 1; a < entrancesNum; a++) {
				if (array[j][a] == 1)
					for (int i = a + 1; i < array.length; i++)
						array[a][i] = array[j][i];
				else
					for (int r = a + 1; r < array.length; r++)
						if (array[j][r] == 1)
							array[a][r] = 0;
			}
		}
		for (int a = 0; a < entrancesNum; a++) {
			if (bits.get(home) == true)
				array[a][a] = 1;
			else
				array[a][a] = 0;
			home++;
		}
		shoro = home;
		return array;
	}

	public int shoroAD = 0;

	public int[][] unCalcForBlockadeAD(BitSet bits, int start,
			HashMap<Integer, Entity> allRanks, StandardWorldModel model,
			Logger logger) {
		int ja = fromMabnaye2ToMabnaye10(bits, start + powNum, start);
		worldGraph.Area area = ((Area) allRanks.get(ja)).worldGraphArea;
		int entrancesNum = area.enterances.size();
		int[][] array = new int[entrancesNum][entrancesNum];
		for (int i = 0; i < array.length; i++)
			for (int j = i; j < array.length; j++)
				array[i][j] = 2;
		int home = start + powNum;
		for (int j = 0; j < entrancesNum; j++) {
			for (int i = j + 1; i < array.length; i++)
				if (array[j][i] == 2) {
					if (bits.get(home) == true)
						array[j][i] = 1;
					else
						array[j][i] = 0;
					home++;
				}
			for (int a = j + 1; a < entrancesNum; a++) {
				if (array[j][a] == 1)
					for (int i = a + 1; i < array.length; i++)
						array[a][i] = array[j][i];
				else
					for (int r = a + 1; r < array.length; r++)
						if (array[j][r] == 1)
							array[a][r] = 0;
			}
		}
		for (int a = 0; a < entrancesNum; a++) {
			if (bits.get(home) == true)
				array[a][a] = 1;
			else
				array[a][a] = 0;
			home++;
		}
		shoroAD = home;
		return array;
	}

	public static void mark(int[][] array) {
		for (int j = 0; j < array.length; j++)
			for (int a = j + 1; a < array.length; a++)
				if (array[j][a] == 1)
					for (int i = a + 1; i < array.length; i++)
						array[a][i] = 2;
				else if (array[j][a] == 0)
					for (int i = a + 1; i < array.length; i++)
						if (array[j][i] == 1)
							array[a][i] = 2;
	}

	// public static boolean checkBooleanArray(ArrayList<Boolean> a,
	// ArrayList<Boolean> b) {
	// boolean ckeck = false;
	// int tedadBararbarha = 0;
	// if (a.size() == b.size()) {
	// for (int x = 0; x < a.size(); x++) {
	// if (a.get(x) == b.get(x))
	// tedadBararbarha++;
	// }
	// }
	// if (tedadBararbarha == a.size())
	// ckeck = true;
	// return ckeck;
	// }

	public static boolean[] changeFromByteArrayToBooleanArray(byte[] byteArray,
			int sizeOfBooleanArray) {
		BitSet bits = fromByteArray(byteArray);
		boolean[] booleanArray = new boolean[sizeOfBooleanArray];
		for (int a = 0; a < booleanArray.length; a++) {
			booleanArray[a] = bits.get(a);
		}
		return booleanArray;
	}

	// ساختار جدید پلیس

	public boolean checkBits(BitSet bits1, BitSet bits2, int taKoja) {
		boolean areTheySame = true;
		for (int i = 0; i < taKoja; i++)
			if (bits1.get(i) != bits2.get(i)) {
				areTheySame = false;
				break;
			}
		return areTheySame;
	}
}
