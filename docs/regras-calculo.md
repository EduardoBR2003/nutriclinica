# Regras de Cálculo Antropométrico

Especificação dos indicadores calculados automaticamente pelo sistema.
Este documento é a fonte da verdade para `CalculoAntropometricoService`.

> Todos os cálculos ocorrem no servidor. O cliente envia apenas valores
> medidos; os derivados são ignorados se vierem na requisição.

---

## 1. IMC — Índice de Massa Corporal

```
IMC = peso_kg / (altura_m)²
```

Altura é armazenada em centímetros; converter antes de calcular.
Arredondar para 2 casas decimais, `BigDecimal`, `RoundingMode.HALF_UP`.

Retorna `null` se peso ou altura estiverem ausentes.

---

## 2. Classificação do IMC

A faixa aplicada depende da idade do paciente na data da consulta.

### 2.1 Adultos (20 a 59 anos) — OMS

| IMC (kg/m²) | Classificação |
|---|---|
| < 18,50 | `BAIXO_PESO` |
| 18,50 – 24,99 | `EUTROFIA` |
| 25,00 – 29,99 | `SOBREPESO` |
| 30,00 – 34,99 | `OBESIDADE_GRAU_I` |
| 35,00 – 39,99 | `OBESIDADE_GRAU_II` |
| ≥ 40,00 | `OBESIDADE_GRAU_III` |

Fonte: WHO. *Obesity: preventing and managing the global epidemic.*
Technical Report Series 894, Geneva, 2000.

### 2.2 Idosos (≥ 60 anos) — Lipschitz

| IMC (kg/m²) | Classificação |
|---|---|
| < 22,00 | `BAIXO_PESO` |
| 22,00 – 27,00 | `EUTROFIA` |
| > 27,00 | `SOBREPESO` |

Fonte: LIPSCHITZ, D. A. *Screening for nutritional status in the elderly.*
Primary Care, v. 21, n. 1, p. 55-67, 1994. Adotado pelo SISVAN/Ministério
da Saúde para a faixa etária.

O idoso perde massa magra e ganha gordura visceral com o mesmo peso, então
a faixa da OMS subestima o risco de desnutrição nessa população. Usar a
tabela de adulto para idoso é erro clínico, não detalhe de implementação.

### 2.3 Menores de 20 anos

Não classificar pelo IMC absoluto. A avaliação usa escore-z das curvas de
crescimento da OMS (IMC/idade), fora do escopo desta versão.
Retornar `null` em `classificacaoImc` e sinalizar
`observacao: "Classificação por escore-z não implementada nesta versão"`.

---

## 3. Relação Cintura/Quadril (RCQ)

```
RCQ = circunferência_cintura_cm / circunferência_quadril_cm
```

Arredondar para 2 casas. Retorna `null` se qualquer medida faltar.

### Interpretação — risco elevado

| Sexo | RCQ com risco elevado |
|---|---|
| Masculino | ≥ 0,90 |
| Feminino | ≥ 0,85 |

Exposto como `rcqElevada` (booleano). **Campo derivado, não persistido** —
calculado na leitura a partir da RCQ armazenada e do sexo do paciente.

Fonte: WHO. *Waist Circumference and Waist–Hip Ratio: Report of a WHO
Expert Consultation.* Geneva, 2008.

Se o sexo do paciente for `OUTRO` ou `NAO_INFORMADO`, retornar `null` —
não assumir um padrão.

---

## 4. Risco Cardiovascular

Derivado da **circunferência da cintura isolada**, não da RCQ. A cintura
estratifica melhor em três níveis e é a medida recomendada pela OMS para
risco metabólico.

| Sexo | `BAIXO` | `AUMENTADO` | `MUITO_AUMENTADO` |
|---|---|---|---|
| Masculino | < 94 cm | 94 – 101,9 cm | ≥ 102 cm |
| Feminino | < 80 cm | 80 – 87,9 cm | ≥ 88 cm |

Fonte: WHO, 2008 (mesma referência acima).

Retorna `null` se a cintura estiver ausente ou se o sexo não for
`MASCULINO` ou `FEMININO`.

> Os pontos de corte acima derivam de população europeia. Para populações
> asiáticas a OMS recomenda limiares menores. Como a clínica escola atende
> população brasileira, mantemos os cortes gerais — mas isso é uma decisão
> explícita, não um descuido, e vale registrar na documentação do projeto.

---

## 5. Massa Magra e Percentual de Gordura

Nesta versão são **entrada manual**, vindos da bioimpedância. O sistema não
estima gordura corporal por dobras cutâneas nem por equações preditivas.

Validação: percentual de gordura entre 1 e 70; massa magra menor que o peso
corporal informado. Divergência gera aviso de validação, não erro fatal.

---

## 6. Regras gerais de implementação

- Use `BigDecimal` em todos os cálculos. `double` produz `24.999999999997`
  em faixas de corte e classifica errado no limite.
- Comparações de faixa são feitas com `compareTo`, nunca `equals`.
- Dados incompletos retornam `null` no indicador correspondente, sem
  lançar exceção. Prontuário meio preenchido é o estado normal durante
  a consulta.
- Cada indicador é independente: faltar quadril não deve impedir o
  cálculo do IMC.
- A idade usada na classificação é a idade **na data da consulta**, não
  a idade atual.
