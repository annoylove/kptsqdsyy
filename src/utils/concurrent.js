/**
 * 并发任务池：限制最大并发数（如上传最大并发 3）。
 * 输入一组返回 Promise 的任务工厂，按并发上限调度，全部完成后返回结果数组（顺序保持）。
 * 单个任务失败不影响其它任务，结果中标记 { status: 'fulfilled' | 'rejected', value/reason }。
 */
export async function runWithConcurrency(taskFactories, limit = 3) {
  const results = new Array(taskFactories.length)
  let cursor = 0

  async function worker() {
    while (cursor < taskFactories.length) {
      const index = cursor++
      try {
        const value = await taskFactories[index]()
        results[index] = { status: 'fulfilled', value, index }
      } catch (reason) {
        results[index] = { status: 'rejected', reason, index }
      }
    }
  }

  const workers = []
  for (let i = 0; i < Math.min(limit, taskFactories.length); i++) {
    workers.push(worker())
  }
  await Promise.all(workers)
  return results
}
