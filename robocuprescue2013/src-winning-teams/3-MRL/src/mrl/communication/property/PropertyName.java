package mrl.communication.property;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 3, 2011
 * Time: 4:57:04 PM
 */
public enum PropertyName {
    PacketCycle,
    PacketType,
    MessageType,
    PacketTypeAndHMessageNumber,
    LMessageNumberAndTTL,

    // scanner message properties
    ChannelIdAndAgentTypeInThisChannel,
    repeatForPriorityOneAndTwo,
    repeatForPriorityThreeAndFour,

    AgentIdIndex,
    AreaIdIndex,

    AgentID,
    HumanID,
    Buriedness,
    HealthPoint,
    HealthPointAndAreaIdIndex,
    Damage,
    TimeToRefuge,

    FierinessAndIndex,
    StateAndIndex,
    Temperature,
    Water,

    PathIdIndex,
    LocationX,
    LocationY,

    NodeId,

    Value,
    Importance,

    ZoneId,
    CivilianCount,
    CivilianID,

    bidVAlue,
    ValueFunction,
    TimeStep,
    TotalATsInThisRescue,
    TotalRescueTime,

    FullBuildingID,
    EmptyBuildingID;
}
