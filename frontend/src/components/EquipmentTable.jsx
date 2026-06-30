export default function EquipmentTable({ equipments }) {
    return (
        <section className="card wide-card">
            <div className="section-title inline-title">
                <div>
                    <p>Equipment Monitor</p>
                    <h2>設備清單</h2>
                </div>
                <span className="pill">{equipments.length} 台設備</span>
            </div>
            <div className="table-shell">
                <table>
                    <thead>
                    <tr>
                        <th>設備代號</th>
                        <th>名稱</th>
                        <th>產線</th>
                        <th>狀態</th>
                    </tr>
                    </thead>
                    <tbody>
                    {equipments.map((equipment) => (
                        <tr key={equipment.equipmentId}>
                            <td>{equipment.equipmentId}</td>
                            <td>{equipment.name}</td>
                            <td>{equipment.factoryArea}</td>
                            <td><span className={`badge ${equipment.status}`}>{equipment.status}</span></td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </section>
    );
}