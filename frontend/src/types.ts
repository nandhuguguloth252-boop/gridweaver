export type BatteryState='IDLE'|'CHARGING'|'DISCHARGING'|'FAULT';
export interface Node {id:string;zone:string;lat:number;lon:number;capacityKwh:number;chargeKwh:number;solarKw:number;loadKw:number;state:BatteryState;health:number;version:number;updatedAt:string;stateOfCharge?:number}
export interface ZoneSummary {zone:string;solarKw:number;loadKw:number;batteryKwh:number;balanceKw:number}
export interface Summary {nodes:number;solarKw:number;loadKw:number;netKw:number;batteryCapacityKwh:number;batteryChargeKwh:number;avgHealth:number;resilience:number;states:Record<BatteryState,number>;zones:ZoneSummary[];events:number;virtualTasksSubmitted:number;virtualTasksCompleted:number;transfers:Record<string,number>}
export interface GridEvent {id:number;nodeId:string;zone:string;fromState:string;toState:string;reason:string;createdAt:string;loadKw:number;solarKw:number}
