// Chart tokens. Categorical slots are the validated reference palette (blue, orange, aqua) in fixed
// order; checked with the dataviz palette validator against the white card surface. Aqua is below 3:1
// contrast, so every chart card offers a table view.
export const SERIES = ['#2a78d6', '#eb6834', '#1baf7a']
export const POS = '#2a78d6' // diverging pair: positive
export const NEG = '#e34948' // diverging pair: negative
export const NEUTRAL = '#c3c2b7'
export const GRID = '#e1e0d9'
export const AXIS = '#898781'

export const axisProps = {
  stroke: AXIS,
  tick: { fill: AXIS, fontSize: 11 },
  tickLine: false,
  axisLine: { stroke: '#c3c2b7' },
}

