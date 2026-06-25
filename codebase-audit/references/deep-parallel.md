# Deep parallel

Max **6** workers. `deep` / 深度 effort only.

## Batches

1. **A:** A2, A3, S1, C0, V1, V2, D1, O1  
2. **B:** A1, A4, A5, A6, C1, C2, C3  

Fallback order: A2 → A3 → S1 → C0 → V1 → V2 → D1 → O1 → A1 → A4 → A5 → A6 → C1 → C2 → C3.

Worker gets invoked `playbook.md` sections + `map.md` + `report.md` finding table. Parent runs **Vet** before posting the report in chat.
