import React from 'react';
export default function KpiCard({label,value,sub}:{label:string;value:string|number;sub?:string}){return <div className="kpi"><div className="kpiLabel">{label}</div><div className="kpiValue">{value}</div>{sub&&<div className="kpiSub">{sub}</div>}</div>}
