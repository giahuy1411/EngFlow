<template>
  <div class="speaking-skill space-y-8">
    <div v-if="!skillContent" class="bg-white border-4 border-black shadow-hard-lg p-10 text-center">
      <div class="text-6xl mb-4 opacity-30">S</div>
      <p class="font-bold text-xl uppercase">No speaking content yet</p>
      <p class="font-bold text-xs uppercase tracking-wider text-foreground/60 mt-2">This lesson doesn't have speaking exercises.</p>
    </div>

    <div v-else>
      <div class="bg-white border-4 border-black shadow-hard-lg">
        <div class="bg-black text-white px-6 py-4 flex items-center gap-3">
          <span class="w-8 h-8 bg-yellow-400 text-black rounded-full flex items-center justify-center font-black text-sm">S</span>
          <h3 class="font-black text-xl uppercase tracking-tight">Speaking</h3>
        </div>
        <div class="px-6 py-6">
          <div v-html="skillContent.content" class="skill-html mb-8"></div>

          <div class="border-t-4 border-black pt-6">
            <h4 class="font-black text-base uppercase mb-4 flex items-center gap-2">
              <span class="w-6 h-6 bg-red-500 border-2 border-black rounded flex items-center justify-center font-black text-xs text-white">&#9835;</span>
              Record Your Speaking
            </h4>

            <AudioRecorder @submitted="onRecorded" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AudioRecorder from '@/components/lessons/AudioRecorder.vue'

const props = defineProps({
  lesson: { type: Object, required: true },
  skills: { type: Array, required: true },
})

const skillContent = computed(() => props.skills.find(s => s.skillType === 'SPEAKING'))

function onRecorded() {
  // Audio submitted via AudioRecorder
}
</script>
