<template>
  <el-card>
    <template #header>通知设置</template>
    <el-form label-width="120px">
      <el-form-item label="系统通知">
        <el-switch v-model="setting.systemNotice" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="考试提醒">
        <el-switch v-model="setting.examRemind" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="组队邀请">
        <el-switch v-model="setting.teamInvite" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="@提醒">
        <el-switch v-model="setting.atRemind" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="回复提醒">
        <el-switch v-model="setting.replyRemind" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="PK邀请">
        <el-switch v-model="setting.pkInvite" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="save">保存设置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getNotificationSetting, updateNotificationSetting } from '@/api/notification'

const setting = reactive({
  systemNotice: 1,
  examRemind: 1,
  teamInvite: 1,
  atRemind: 1,
  replyRemind: 1,
  pkInvite: 1
})

onMounted(async () => {
  const res: any = await getNotificationSetting()
  if (res.data) Object.assign(setting, res.data)
})

async function save() {
  await updateNotificationSetting(setting)
  ElMessage.success('已保存')
}
</script>
