<template>
  <div class="report-page">
    <div class="page-header header-indigo">
      <h2>学情报告</h2>
    </div>

    <!-- 概览统计 -->
    <el-row :gutter="16" class="summary-row" v-if="summary">
      <el-col :span="12">
        <el-card shadow="hover" class="accent-jade">
          <div class="stat-card">
            <div class="stat-value num-jade">{{ summary.totalExams }}</div>
            <div class="stat-label">总考试次数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="accent-amber">
          <div class="stat-card">
            <div class="stat-value num-amber">{{ summary.avgScore }}</div>
            <div class="stat-label">平均分</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" class="charts-row">
      <!-- 能力雷达图 -->
      <el-col :span="12">
        <el-card shadow="hover" v-loading="loading" class="accent-indigo">
          <template #header>
            <span class="card-title">个人能力模型</span>
          </template>
          <div ref="radarChartRef" class="chart-box" style="height:360px"></div>
          <div v-if="!radarData || !radarData.indicators || radarData.indicators.length === 0" class="empty-data">
            暂无数据，参加考试后生成
          </div>
        </el-card>
      </el-col>

      <!-- 成绩趋势图 -->
      <el-col :span="12">
        <el-card shadow="hover" v-loading="loading" class="accent-indigo">
          <template #header>
            <span class="card-title">考试成绩趋势</span>
          </template>
          <div ref="trendChartRef" class="chart-box" style="height:360px"></div>
          <div v-if="!trendData || trendData.length === 0" class="empty-data">
            暂无数据，参加考试后生成
          </div>
        </el-card>
      </el-col>
    </el-row>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getPersonalReport } from '@/api/report'
import { getMyRecords } from '@/api/exam'
import request from '@/utils/request'

const loading = ref(true)

// 数据
const summary = ref<any>(null)
const radarData = ref<any>(null)
const trendData = ref<any>(null)

// 图表引用
const radarChartRef = ref<HTMLDivElement | null>(null)
const trendChartRef = ref<HTMLDivElement | null>(null)

let radarChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

function renderRadarChart() {
  if (!radarChartRef.value || !radarData.value) return
  if (!radarChart) {
    radarChart = echarts.init(radarChartRef.value)
  }
  const d = radarData.value
  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'item' },
    legend: { data: ['我的能力'], bottom: 0 },
    radar: {
      indicator: d.indicators.map((name: string) => ({
        name,
        max: 100
      })),
      center: ['50%', '45%'],
      radius: '65%'
    },
    series: [{
      type: 'radar',
      name: '我的能力',
      data: [{ value: d.values, name: '我的能力' }],
      areaStyle: { color: 'rgba(5, 150, 105, 0.15)' },
      lineStyle: { color: '#059669' },
      itemStyle: { color: '#059669' }
    }]
  }
  radarChart.setOption(option)
}

function renderTrendChart() {
  if (!trendChartRef.value || !trendData.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const trendList = trendData.value
  if (!trendList || trendList.length === 0) {
    trendChart.setOption({
      title: { text: '暂无考试数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } }
    })
    return
  }
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        return `${p.axisValue}<br/>得分：${p.value} 分`
      }
    },
    xAxis: {
      type: 'category',
      data: trendList.map((t: any) => {
        const time = t.submit_time || ''
        return time.length >= 10 ? time.substring(0, 10) : time
      }),
      axisLabel: { rotate: 30 }
    },
    yAxis: { type: 'value', name: '分数' },
    grid: { left: 50, right: 30, top: 30, bottom: 60 },
    series: [{
      type: 'line',
      data: trendList.map((t: any) => {
        const s = t.total_score
        return s != null ? Number(s) : 0
      }),
      smooth: true,
      lineStyle: { color: '#67C23A', width: 3 },
      itemStyle: { color: '#67C23A' },
      areaStyle: { color: 'rgba(103, 194, 58, 0.1)' },
      markLine: {
        silent: true,
        data: [{ type: 'average', name: '平均分' }],
        lineStyle: { color: '#E6A23C', type: 'dashed' }
      }
    }]
  }
  trendChart.setOption(option)
}

async function loadData() {
  loading.value = true
  try {
    // 加载个人报告主数据
    const res = await getPersonalReport()
    const data = res.data
    if (data) {
      radarData.value = data.radarData
      summary.value = data.summary
      trendData.value = data.trendData
    }
    // 如果没有趋势数据，尝试从 /exam/my-records 补充
    if (!trendData.value || trendData.value.length === 0) {
      try {
        const recordsRes = await getMyRecords({ current: 1, size: 50 })
        if (recordsRes.data?.records?.length > 0) {
          trendData.value = recordsRes.data.records.map((r: any) => ({
            submit_time: r.submitTime,
            total_score: r.totalScore
          }))
        }
      } catch { /* ignore */ }
    }
    // 尝试从 /user/my-stats 补充概览数据
    if (!summary.value) {
      try {
        const statsRes = await request.get('/user/my-stats')
        if (statsRes.data) {
          summary.value = statsRes.data
        }
      } catch { /* ignore */ }
    }
    await nextTick()
    renderRadarChart()
    renderTrendChart()
  } finally {
    loading.value = false
  }
}

// 窗口大小变化时重绘图表
window.addEventListener('resize', () => {
  radarChart?.resize()
  trendChart?.resize()
})

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.report-page { padding: 24px; max-width: 1400px; margin: 0 auto; }

.summary-row { margin-bottom: 16px; }
.stat-card { text-align: center; padding: 8px 0; }
.stat-value { font-size: 32px; font-weight: bold; }
.stat-label { font-size: 14px; color: var(--color-ink-muted); margin-top: 4px; }

.charts-row { margin-bottom: 16px; }
.card-title { font-weight: 600; font-size: 15px; }
.chart-box { width: 100%; }
.empty-data { text-align: center; color: var(--color-ink-muted); padding: 40px 0; font-size: 14px; }
</style>
