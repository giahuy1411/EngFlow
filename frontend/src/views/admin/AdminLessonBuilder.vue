<template>
  <div class="space-y-8">
    <!-- Header -->
    <div class="flex items-center justify-between flex-wrap gap-4">
      <div class="flex items-center gap-4">
        <div class="w-10 h-10 bg-accent border-2 border-foreground flex items-center justify-center rotate-6 rounded-md">
          <span class="text-white font-black text-sm">B</span>
        </div>
        <div>
          <h2 class="font-black text-2xl uppercase tracking-tighter">Xây Dựng Bài Học</h2>
          <p class="font-bold text-xs uppercase tracking-widest text-muted-foreground">{{ lesson?.title || 'Đang tải...' }}</p>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <AppButton @click="saveAll" :disabled="saving || dirtyBlocks.size === 0"
                :loading="saving" variant="primary">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <path d="M5 13l4 4L19 7"/>
          </svg>
          {{ saving ? 'Đang lưu...' : `Lưu tất cả${dirtyBlocks.size > 0 ? ' (' + dirtyBlocks.size + ')' : ''}` }}
        </AppButton>
        <AppButton @click="toggleHistory"
                :variant="showHistory ? 'tertiary' : 'secondary'">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
          </svg>
          Lịch sử
        </AppButton>
        <AppButton as="a" :href="'/lessons/' + lessonId + '/preview'" target="_blank" variant="pink">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
          </svg>
          Xem trước
        </AppButton>
      </div>
    </div>

    <!-- Save status toast -->
    <div v-if="saveStatus === 'success'" class="bg-success/10 border-2 border-foreground rounded-md p-4 flex items-center gap-3 font-bold text-sm shadow-pop-sm">
      <span class="w-6 h-6 bg-quaternary text-foreground rounded-full flex items-center justify-center text-xs font-black">&#10003;</span>
      Đã lưu thành công
    </div>
    <div v-else-if="saveStatus === 'error'" class="bg-danger/10 border-2 border-foreground rounded-md p-4 flex items-center gap-3 font-bold text-sm shadow-pop-sm">
      <span class="w-6 h-6 bg-accent text-white rounded-full flex items-center justify-center text-xs font-black">&#10007;</span>
      Lỗi lưu. Vui lòng thử lại.
    </div>

    <!-- Version History Panel -->
    <div v-if="showHistory" class="bg-white border-2 border-foreground shadow-pop-lg rounded-md overflow-hidden">
      <div class="bg-foreground text-white px-5 py-3 flex items-center justify-between">
        <span class="font-black uppercase text-sm tracking-wider">Lịch sử phiên bản</span>
        <span class="text-xs text-white/60">{{ snapshots.length }} bản ghi</span>
      </div>
      <div class="p-4">
        <div v-if="loadingSnapshots" class="text-center py-8 text-muted-foreground font-bold text-sm uppercase tracking-wider">
          Đang tải...
        </div>
        <div v-else-if="snapshots.length === 0" class="text-center py-8 text-muted-foreground font-bold text-sm uppercase tracking-wider">
          Chưa có phiên bản nào
        </div>
        <div v-else class="space-y-2">
          <div v-for="snap in snapshots" :key="snap.id"
               class="flex items-center justify-between border-2 border-foreground rounded-md p-3 bg-accent/5">
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 bg-foreground/10 flex items-center justify-center rounded-lg">
                <svg class="w-4 h-4 text-foreground/40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
                </svg>
              </div>
              <span class="font-bold text-sm">{{ formatDate(snap.createdAt) }}</span>
            </div>
            <AppButton @click="restoreSnapshot(snap.id)" :disabled="restoring === snap.id"
                    :loading="restoring === snap.id" size="sm" variant="secondary">
              {{ restoring === snap.id ? 'Đang khôi phục...' : 'Khôi phục' }}
            </AppButton>
          </div>
        </div>
      </div>
    </div>

    <!-- Loading state -->
    <div v-if="loading" class="text-center py-24">
      <div class="animate-spin inline-block w-10 h-10 border-2 border-foreground border-t-accent rounded-full"></div>
      <p class="font-bold uppercase mt-4 tracking-wider text-muted-foreground">Đang tải cấu trúc bài học...</p>
    </div>

    <!-- Empty state -->
    <div v-else-if="sections.length === 0 && !loading" class="bg-white border-2 border-foreground shadow-pop-lg rounded-md p-16 text-center">
      <div class="w-16 h-16 bg-tertiary border-2 border-foreground rounded-md flex items-center justify-center mx-auto mb-6 rotate-3">
        <span class="text-3xl font-black text-foreground">+</span>
      </div>
      <p class="font-black text-xl uppercase mb-2">Chưa có nội dung</p>
      <p class="text-muted-foreground font-medium mb-6">Bắt đầu bằng cách thêm section và block</p>
      <AppButton @click="addSection" variant="primary">
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        Thêm Section Đầu Tiên
      </AppButton>
    </div>

    <!-- Builder -->
    <template v-else>
      <div class="space-y-6">
        <div v-for="(section, si) in sections" :key="section.id"
             class="bg-white border-2 border-foreground shadow-pop-lg rounded-md overflow-hidden transition-all duration-300"
             :class="{ 'opacity-50': deletingSection === section.id }">
          <!-- Section Header -->
          <div class="bg-foreground border-b-2 border-foreground flex items-stretch">
            <div class="bg-tertiary text-foreground px-4 py-3 flex items-center justify-center font-black text-lg border-r-4 border-foreground min-w-[48px]">
              {{ si + 1 }}
            </div>
            <input v-model="section.title" @change="updateSection(section)"
                   class="flex-1 bg-transparent text-white font-bold text-lg px-4 py-3 focus:outline-none focus:bg-white/10 transition-colors"
                    placeholder="Tiêu đề section..." />
            <div class="flex items-stretch">
              <AppButton @click="addBlock(section.id)" variant="ghost" size="sm">
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg> Block
              </AppButton>
              <!-- icon-only control: kept raw -->
              <button @click="confirmDeleteSection(section, si)" :aria-label="'Xóa section ' + (si + 1)"
                      class="px-4 border-l-4 border-foreground text-white/40 hover:text-accent hover:bg-white/10 font-bold text-lg transition-all flex items-center">
                &times;
              </button>
            </div>
          </div>

          <!-- Section Body (Blocks) -->
          <div v-if="section.blocks && section.blocks.length > 0" class="p-4 space-y-4">
            <div v-for="(block, bi) in section.blocks" :key="block.id"
                 class="relative">
              <!-- Block Card -->
              <div class="border-2 border-foreground rounded-md overflow-hidden shadow-pop-sm"
                   :class="[blockAccent[block.blockType]?.container || 'bg-white', dirtyBlocks.has(block.id) ? 'ring-4 ring-tertiary border-tertiary' : '']">
                <!-- Block Header -->
                <div class="flex items-center justify-between px-4 py-2.5 border-b-2 border-foreground"
                     :class="blockAccent[block.blockType]?.header || 'bg-muted'">
                  <div class="flex items-center gap-2.5">
                    <span class="w-6 h-6 flex items-center justify-center rounded-full text-xs font-black"
                          :class="blockAccent[block.blockType]?.badge || 'bg-muted/600'">
                      {{ blockAccent[block.blockType]?.icon || '?' }}
                    </span>
                    <span class="font-bold text-xs uppercase tracking-widest"
                          :class="blockAccent[block.blockType]?.labelClass || 'text-muted-foreground'">
                      {{ blockLabels[block.blockType] || block.blockType }}
                    </span>
                    <select v-model="block.blockType" @change="onBlockTypeChange(block)"
                            class="ml-2 text-[10px] uppercase font-bold tracking-wider border-2 border-foreground rounded-lg px-2 py-0.5 bg-white
                                   focus:outline-none focus:ring-2 focus:ring-accent transition-all appearance-none cursor-pointer shadow-pop-sm">
                      <option v-for="opt in blockTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                    </select>
                    <span v-if="dirtyBlocks.has(block.id)"
                          class="ml-1 w-2 h-2 rounded-full bg-tertiary inline-block"></span>
                  </div>
                  <!-- icon-only control: kept raw -->
                  <button @click="deleteBlock(block.id, section.id, bi)" :aria-label="'Xóa block ' + (bi + 1)"
                          class="w-7 h-7 flex items-center justify-center border-2 border-foreground rounded-lg bg-white text-foreground/40 hover:text-accent hover:bg-accent/10 font-bold text-sm transition-all shadow-pop-sm">
                    &times;
                  </button>
                </div>

                <!-- Block Content -->
                <div class="p-4">
                  <!-- TEXT -->
                  <div v-if="block.blockType === 'TEXT'" class="space-y-2">
                    <textarea v-model="blockData[block.id].content" @input="markDirty(block)"
                              class="w-full border-2 border-foreground rounded-md p-4 bg-muted/60 font-mono text-sm leading-relaxed min-h-[400px] focus:outline-none focus:ring-2 focus:ring-accent focus:bg-white transition-all resize-y shadow-inner"
                               placeholder="<p>Nhập mã HTML hoặc văn bản vào đây...</p>"></textarea>
                  </div>

                  <!-- IMAGE -->
                  <div v-else-if="block.blockType === 'IMAGE'" class="space-y-3">
                    <div class="flex gap-2">
                      <div class="flex-1 relative">
                        <input v-model="blockData[block.id].imageUrl" @input="markDirty(block)"
                               class="w-full border-2 border-foreground rounded-md p-2.5 bg-white font-bold text-sm focus:outline-none focus:ring-2 focus:ring-accent transition-all"
                               placeholder="https://example.com/image.jpg" />
                      </div>
                      <label class="flex items-center gap-1.5 px-4 py-2.5 bg-secondary text-foreground border-2 border-foreground rounded-md font-black text-xs uppercase tracking-wider cursor-pointer
                                    hover:-translate-y-0.5 hover:shadow-pop-sm active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all shadow-pop-sm">
                        <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                        Tải lên
                        <input type="file" accept="image/*" class="hidden" @change="uploadFile($event, block)" />
                      </label>
                    </div>
                    <input v-model="blockData[block.id].caption" @input="markDirty(block)"
                           class="w-full border-2 border-foreground rounded-md p-2 bg-white font-medium text-sm focus:outline-none focus:ring-2 focus:ring-accent transition-all"
                           placeholder="Chú thích (tùy chọn)..." />
                    <div v-if="blockData[block.id].imageUrl" class="border-2 border-foreground rounded-md bg-muted/60 p-2 overflow-hidden shadow-pop-sm">
                      <img :src="blockData[block.id].imageUrl" class="max-h-48 mx-auto object-contain" />
                      <p v-if="blockData[block.id].caption" class="text-center font-bold text-xs uppercase tracking-wider mt-2 text-muted-foreground">{{ blockData[block.id].caption }}</p>
                    </div>
                  </div>

                  <!-- AUDIO -->
                  <div v-else-if="block.blockType === 'AUDIO'" class="space-y-3">
                    <div class="flex gap-2">
                      <input v-model="blockData[block.id].audioUrl" @input="markDirty(block)"
                             class="flex-1 border-2 border-foreground rounded-md p-2.5 bg-white font-bold text-sm focus:outline-none focus:ring-2 focus:ring-accent transition-all"
                             placeholder="https://example.com/audio.mp3" />
                      <label class="flex items-center gap-1.5 px-4 py-2.5 bg-secondary text-foreground border-2 border-foreground rounded-md font-black text-xs uppercase tracking-wider cursor-pointer
                                    hover:-translate-y-0.5 hover:shadow-pop-sm active:translate-x-0.5 active:translate-y-0.5 active:shadow-none transition-all shadow-pop-sm">
                        <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                        Tải lên
                        <input type="file" accept="audio/*" class="hidden" @change="uploadFile($event, block, 'audioUrl')" />
                      </label>
                    </div>
                    <input v-model="blockData[block.id].transcript" @input="markDirty(block)"
                           class="w-full border-2 border-foreground rounded-md p-2 bg-white font-medium text-sm focus:outline-none focus:ring-2 focus:ring-accent transition-all"
                           placeholder="Transcript (tùy chọn)..." />
                    <audio v-if="blockData[block.id].audioUrl" :src="blockData[block.id].audioUrl" controls class="w-full h-10" />
                  </div>

                  <!-- TABLE -->
                  <div v-else-if="block.blockType === 'TABLE'" class="space-y-3">
                    <div>
                      <label :for="`lb-headers-${block.id}`" class="block font-bold uppercase text-[10px] tracking-widest text-muted-foreground mb-1">Headers</label>
                      <input :id="`lb-headers-${block.id}`" v-model="blockData[block.id].headersStr" @input="markDirty(block)"
                             class="w-full border-2 border-foreground rounded-md p-2 bg-white font-medium text-sm focus:outline-none focus:ring-2 focus:ring-accent transition-all"
                             placeholder="col1, col2, col3" />
                    </div>
                    <div>
                      <label :for="`lb-rows-${block.id}`" class="block font-bold uppercase text-[10px] tracking-widest text-muted-foreground mb-1">Rows</label>
                      <textarea :id="`lb-rows-${block.id}`" v-model="blockData[block.id].rowsStr" @input="markDirty(block)"
                                class="w-full border-2 border-foreground rounded-md p-2 bg-white font-mono text-sm min-h-[80px] focus:outline-none focus:ring-2 focus:ring-accent transition-all resize-y"
                                placeholder="a | b | c"></textarea>
                    </div>
                    <!-- Table Preview -->
                    <div v-if="blockData[block.id].headersStr || blockData[block.id].rowsStr" class="border-2 border-foreground rounded-md overflow-hidden bg-white shadow-pop-sm">
                      <table class="w-full text-left border-collapse">
                        <thead v-if="blockData[block.id].headersStr" class="bg-foreground text-white">
                          <tr>
                            <th v-for="h in parseHeaders(blockData[block.id].headersStr)" :key="h"
                                class="px-4 py-2 font-bold text-xs uppercase tracking-wider border-r-2 border-white/20 last:border-r-0">{{ h }}</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="(row, ri) in parseRows(blockData[block.id].rowsStr)" :key="ri"
                              class="border-t-2 border-foreground even:bg-muted/60">
                            <td v-for="(cell, ci) in row" :key="ci"
                                class="px-4 py-2 font-medium text-sm border-r-2 border-foreground/20 last:border-r-0">{{ cell }}</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>

                  <!-- QUESTION -->
                  <div v-else-if="block.blockType === 'QUESTION'" class="space-y-3">
                    <div class="flex gap-2">
                      <input v-model="blockData[block.id].questionText" @input="markDirty(block)"
                             class="flex-1 border-2 border-foreground rounded-md p-2.5 bg-white font-bold text-sm focus:outline-none focus:ring-2 focus:ring-accent transition-all"
                             placeholder="Câu hỏi..." />
                      <select v-model="blockData[block.id].questionType" @change="markDirty(block)"
                              class="border-2 border-foreground rounded-md px-2 py-1.5 bg-white font-bold uppercase text-[10px] tracking-wider
                                     focus:outline-none focus:ring-2 focus:ring-accent appearance-none cursor-pointer shadow-pop-sm">
                         <option value="MULTIPLE_CHOICE">Trắc nghiệm</option>
                        <option value="FILL_IN_BLANK">Điền từ</option>
                        <option value="TRUE_FALSE">Đúng/Sai</option>
                         <option value="MATCHING">Nối từ</option>
                      </select>
                    </div>

                    <div v-if="blockData[block.id].questionType === 'MULTIPLE_CHOICE'">
                      <label :for="`lb-options-${block.id}`" class="block font-bold uppercase text-[10px] tracking-widest text-muted-foreground mb-1">Options</label>
                      <textarea :id="`lb-options-${block.id}`" v-model="blockData[block.id].optionsStr" @input="markDirty(block)"
                                class="w-full border-2 border-foreground rounded-md p-2 bg-white font-medium text-sm min-h-[60px] focus:outline-none focus:ring-2 focus:ring-accent transition-all resize-y"
                                placeholder="Đáp án A&#10;Đáp án B&#10;Đáp án C"></textarea>
                    </div>

                    <div class="grid grid-cols-2 gap-3">
                      <input v-model="blockData[block.id].correctAnswer" @input="markDirty(block)"
                             class="border-2 border-foreground rounded-md p-2 bg-white font-bold text-sm focus:outline-none focus:ring-2 focus:ring-accent transition-all"
                              placeholder="Đáp án đúng..." />
                      <input v-model="blockData[block.id].explanation" @input="markDirty(block)"
                             class="border-2 border-foreground rounded-md p-2 bg-white font-medium text-sm focus:outline-none focus:ring-2 focus:ring-accent transition-all"
                             placeholder="Giải thích..." />
                    </div>

                    <div class="flex gap-2">
                      <input v-model="blockData[block.id].imageUrl" @input="markDirty(block)"
                             class="flex-1 border-2 border-foreground rounded-md p-2 bg-white font-medium text-sm focus:outline-none focus:ring-2 focus:ring-accent transition-all"
                             placeholder="URL hình ảnh (tùy chọn)..." />
                      <input v-model="blockData[block.id].audioUrl" @input="markDirty(block)"
                             class="flex-1 border-2 border-foreground rounded-md p-2 bg-white font-medium text-sm focus:outline-none focus:ring-2 focus:ring-accent transition-all"
                             placeholder="URL audio (tùy chọn)..." />
                    </div>
                  </div>

                  <!-- SUBMISSION -->
                  <div v-else-if="block.blockType === 'SUBMISSION'" class="space-y-3">
                    <textarea v-model="blockData[block.id].prompt" @input="markDirty(block)"
                              class="w-full border-2 border-foreground rounded-md p-3 bg-white font-medium text-sm min-h-[80px] focus:outline-none focus:ring-2 focus:ring-accent transition-all resize-y"
                              placeholder="Yêu cầu bài tập..."></textarea>
                    <select v-model="blockData[block.id].submissionType" @change="markDirty(block)"
                            class="border-2 border-foreground rounded-md px-3 py-2 bg-white font-bold uppercase text-xs tracking-wider
                                   focus:outline-none focus:ring-2 focus:ring-accent appearance-none cursor-pointer shadow-pop-sm">
                      <option value="TEXT">Text</option>
                      <option value="AUDIO">Audio</option>
                    </select>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty section -->
          <div v-else class="p-8 text-center">
            <p class="font-bold text-sm uppercase tracking-wider text-muted-foreground mb-3">Section trống</p>
            <AppButton @click="addBlock(section.id)" variant="secondary" size="sm">
              + Thêm Block
            </AppButton>
          </div>
        </div>
      </div>

      <!-- Add Section Button -->
      <div class="flex justify-center pt-4">
        <AppButton @click="addSection" variant="tertiary" size="lg">
          <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Thêm Section
        </AppButton>
      </div>
    </template>

    <!-- Delete Section Modal -->
    <div v-if="deleteTarget" class="fixed inset-0 z-50 flex items-center justify-center bg-foreground/60 backdrop-blur-sm p-4" @click.self="deleteTarget = null">
      <div class="bg-white border-2 border-foreground w-full max-w-sm shadow-pop-xl rounded-md overflow-hidden">
        <div class="bg-accent border-b-2 border-foreground p-5 flex items-center gap-3">
          <div class="w-3 h-3 bg-white rotate-45 rounded"></div>
          <h3 class="font-black text-lg uppercase tracking-tighter text-white">Xóa Section</h3>
        </div>
        <div class="p-6">
          <p class="font-bold mb-2">Xóa section <span class="text-accent">"{{ deleteTarget.section.title }}"</span>?</p>
          <p class="text-muted-foreground text-sm font-medium mb-6">Tất cả block trong section này cũng sẽ bị xóa.</p>
          <div class="flex justify-end gap-4">
            <AppButton @click="deleteTarget = null" variant="secondary">
              Hủy
            </AppButton>
            <AppButton @click="executeDeleteSection" variant="primary">
              Xóa
            </AppButton>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import AppButton from '@/components/ui/AppButton.vue'
import lessonStructureService from '../../services/lessonStructureService'
import lessonService from '../../services/lessonService'

const route = useRoute()
const lessonId = Number(route.params.id)
const lesson = ref(null)
const sections = ref([])
const loading = ref(true)
const blockData = reactive({})
const deletingSection = ref(null)
const deleteTarget = ref(null)

const dirtyBlocks = ref(new Set())
const saving = ref(false)
const saveStatus = ref(null)
let saveStatusTimer = null

const showHistory = ref(false)
const snapshots = ref([])
const loadingSnapshots = ref(false)
const restoring = ref(null)

const blockLabels = {
  TEXT: 'Văn bản',
  IMAGE: 'Hình ảnh',
  AUDIO: 'Audio',
  TABLE: 'Bảng',
  QUESTION: 'Câu hỏi',
  SUBMISSION: 'Bài nộp'
}

const blockTypeOptions = [
  { value: 'TEXT', label: 'Text' },
  { value: 'IMAGE', label: 'Image' },
  { value: 'AUDIO', label: 'Audio' },
  { value: 'TABLE', label: 'Table' },
  { value: 'QUESTION', label: 'Question' },
  { value: 'SUBMISSION', label: 'Submission' }
]

const blockAccent = {
  TEXT: { container: 'border-l-4 border-l-accent/10', header: 'bg-accent/10', badge: 'bg-accent/10 text-foreground font-black', icon: 'T', labelClass: 'text-foreground font-black' },
  IMAGE: { container: 'border-l-4 border-l-secondary', header: 'bg-secondary/10', badge: 'bg-secondary text-foreground font-black', icon: 'I', labelClass: 'text-foreground font-black' },
  AUDIO: { container: 'border-l-4 border-l-accent', header: 'bg-accent/10', badge: 'bg-accent text-white font-black', icon: 'A', labelClass: 'text-foreground font-black' },
  TABLE: { container: 'border-l-4 border-l-secondary', header: 'bg-secondary/10', badge: 'bg-secondary text-foreground font-black', icon: '#', labelClass: 'text-foreground font-black' },
  QUESTION: { container: 'border-l-4 border-l-tertiary', header: 'bg-tertiary/10', badge: 'bg-tertiary text-foreground font-black', icon: '?', labelClass: 'text-foreground font-black' },
  SUBMISSION: { container: 'border-l-4 border-l-secondary', header: 'bg-secondary/10', badge: 'bg-secondary text-white font-black', icon: 'S', labelClass: 'text-foreground font-black' }
}

onMounted(async () => {
  await loadLesson()
  await loadStructure()
  loading.value = false
})

async function loadLesson() {
  try {
    const lessons = await lessonService.getAll()
    lesson.value = lessons.find(l => l.id === lessonId)
  } catch (e) {
    console.error('Failed to load lesson', e)
  }
}

async function loadStructure() {
  try {
    sections.value = await lessonStructureService.getAdminStructure(lessonId)
    sections.value.forEach(s => {
      s.blocks.forEach(b => {
        try { blockData[b.id] = JSON.parse(b.data) } catch { blockData[b.id] = { content: b.data || '' } }
        normalizeBlockData(blockData[b.id])
      })
    })
  } catch (e) {
    console.error('Failed to load structure', e)
    sections.value = []
  }
}

function normalizeBlockData(d) {
  if (d.options && Array.isArray(d.options)) d.optionsStr = d.options.join('\n')
  if (d.headers && Array.isArray(d.headers)) d.headersStr = d.headers.join(', ')
  if (d.rows && Array.isArray(d.rows)) d.rowsStr = d.rows.map(r => r.join(' | ')).join('\n')
}

function serializeBlockData(d) {
  const out = { ...d }
  if (out.optionsStr) {
    out.options = out.optionsStr.split('\n').filter(s => s.trim())
    delete out.optionsStr
  }
  if (out.headersStr) {
    out.headers = out.headersStr.split(',').map(s => s.trim()).filter(s => s)
    delete out.headersStr
  }
  if (out.rowsStr) {
    out.rows = out.rowsStr.split('\n').filter(s => s.trim()).map(r => r.split('|').map(c => c.trim()))
    delete out.rowsStr
  }
  return out
}

function parseHeaders(str) {
  if (!str) return []
  return str.split(',').map(s => s.trim()).filter(s => s)
}

function parseRows(str) {
  if (!str) return []
  return str.split('\n').filter(s => s.trim()).map(r => r.split('|').map(c => c.trim()))
}

function markDirty(block) {
  dirtyBlocks.value = new Set([...dirtyBlocks.value, block.id])
}

function onBlockTypeChange(block) {
  blockData[block.id] = {}
  markDirty(block)
}

async function uploadFile(event, block, field) {
  const file = event.target.files[0]
  if (!file) return
  try {
    const result = await lessonStructureService.uploadFile(file)
    blockData[block.id][field || 'imageUrl'] = result.url
    markDirty(block)
  } catch (e) {
    console.error('Upload failed', e)
  }
}

async function saveAll() {
  if (saving.value || dirtyBlocks.value.size === 0) return
  saving.value = true
  saveStatus.value = null
  const blocks = [...dirtyBlocks.value]
  let failed = false

  for (const blockId of blocks) {
    const block = findBlock(blockId)
    if (!block) continue
    const data = serializeBlockData(blockData[block.id])
    try {
      await lessonStructureService.updateBlock(block.id, {
        blockType: block.blockType,
        data: JSON.stringify(data)
      })
    } catch (e) {
      console.error('Failed to save block', e)
      failed = true
    }
  }

  if (!failed) {
    dirtyBlocks.value = new Set()
    try {
      await lessonStructureService.takeSnapshot(lessonId)
      if (showHistory.value) await loadSnapshots()
    } catch (e) {
      console.error('Failed to take snapshot', e)
    }
  }

  saving.value = false
  saveStatus.value = failed ? 'error' : 'success'

  if (saveStatusTimer) clearTimeout(saveStatusTimer)
  saveStatusTimer = setTimeout(() => { saveStatus.value = null }, 3000)
}

function findBlock(blockId) {
  for (const s of sections.value) {
    const b = s.blocks.find(b => b.id === blockId)
    if (b) return b
  }
  return null
}

async function addSection() {
  try {
    const section = await lessonStructureService.addSection(lessonId, {
      title: 'Section mới',
      orderIndex: (sections.value.length + 1) * 10
    })
    section.blocks = []
    sections.value.push(section)
  } catch (e) {
    console.error('Failed to add section', e)
  }
}

async function updateSection(section) {
  try {
    await lessonStructureService.updateSection(section.id, {
      title: section.title,
      orderIndex: section.orderIndex
    })
  } catch (e) {
    console.error('Failed to update section', e)
  }
}

function confirmDeleteSection(section, index) {
  deleteTarget.value = { section, index }
}

async function executeDeleteSection() {
  if (!deleteTarget.value) return
  const { section, index } = deleteTarget.value
  deletingSection.value = section.id
  try {
    await lessonStructureService.deleteSection(section.id)
    sections.value.splice(index, 1)
    dirtyBlocks.value = new Set([...dirtyBlocks.value].filter(id => {
      return sections.value.some(s => s.blocks.some(b => b.id === id))
    }))

  } catch (e) {
    console.error('Failed to delete section', e)
  }
  deletingSection.value = null
  deleteTarget.value = null
}

async function addBlock(sectionId) {
  try {
    const section = await lessonStructureService.addBlock(sectionId, {
      blockType: 'TEXT',
      data: JSON.stringify({ content: '' }),
      orderIndex: 10
    })
    const idx = sections.value.findIndex(s => s.id === sectionId)
    if (idx !== -1) sections.value[idx] = section
    section.blocks.forEach(b => {
      if (!blockData[b.id]) {
        try { blockData[b.id] = JSON.parse(b.data) } catch { blockData[b.id] = { content: '' } }
      }
      normalizeBlockData(blockData[b.id])
    })
  } catch (e) {
    console.error('Failed to add block', e)
  }
}

async function deleteBlock(blockId, sectionId, index) {
  try {
    await lessonStructureService.deleteBlock(blockId)
    const section = sections.value.find(s => s.id === sectionId)
    if (section) section.blocks.splice(index, 1)
    dirtyBlocks.value = new Set([...dirtyBlocks.value].filter(id => id !== blockId))
  } catch (e) {
    console.error('Failed to delete block', e)
  }
}

async function loadSnapshots() {
  loadingSnapshots.value = true
  try {
    snapshots.value = await lessonStructureService.getSnapshots(lessonId)
  } catch (e) {
    console.error('Failed to load snapshots', e)
  }
  loadingSnapshots.value = false
}

async function restoreSnapshot(snapshotId) {
  if (!confirm('Khôi phục phiên bản này? Các thay đổi chưa lưu sẽ bị mất.')) return
  restoring.value = snapshotId
  try {
    const result = await lessonStructureService.restoreSnapshot(lessonId, snapshotId)
    sections.value = result
    Object.keys(blockData).forEach(k => delete blockData[k])
    sections.value.forEach(s => {
      s.blocks.forEach(b => {
        try { blockData[b.id] = JSON.parse(b.data) } catch { blockData[b.id] = { content: b.data || '' } }
        normalizeBlockData(blockData[b.id])
      })
    })
    dirtyBlocks.value = new Set()
    saveStatus.value = 'success'
    setTimeout(() => { saveStatus.value = null }, 3000)
  } catch (e) {
    console.error('Failed to restore snapshot', e)
    saveStatus.value = 'error'
    setTimeout(() => { saveStatus.value = null }, 3000)
  }
  restoring.value = null
}

function formatDate(dateStr) {
  const d = new Date(dateStr)
  return d.toLocaleString('vi-VN', {
    hour: '2-digit', minute: '2-digit',
    day: '2-digit', month: '2-digit', year: 'numeric'
  })
}

function toggleHistory() {
  showHistory.value = !showHistory.value
  if (showHistory.value) loadSnapshots()
}


</script>


