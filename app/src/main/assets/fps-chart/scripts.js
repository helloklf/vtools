
const SceneJS = window.SceneJS;

function dateFormat(timeLong, fmt) {
  if (!timeLong) {
    return ''
  }

  let date = new Date(timeLong)
  let ret;
  const opt = {
    "Y+": date.getFullYear().toString(),        // 年
    "m+": (date.getMonth() + 1).toString(),     // 月
    "d+": date.getDate().toString(),            // 日
    "H+": date.getHours().toString(),           // 时
    "M+": date.getMinutes().toString(),         // 分
    "S+": date.getSeconds().toString()          // 秒
    // 有其他格式化字符需求可以继续添加，必须转化成字符串
  };
  for (let k in opt) {
    ret = new RegExp("(" + k + ")").exec(fmt);
    if (ret) {
      fmt = fmt.replace(ret[1], (ret[1].length == 1) ? (opt[k]) : (opt[k].padStart(ret[1].length, "0")))
    };
  };
  return fmt;
}

var stackedLine;
var app = new Vue({
  el: '#app',
  data: {
    toolbarOn: SceneJS ? JSON.parse(SceneJS.getFpsToolbarState()) : false,
    sessions: SceneJS ? JSON.parse(SceneJS.getSessions()) : [],
    detail: {
      max: 0,
      min: 0,
      avg: 0,
      fluency: '--',
      highTempRatio: '--',
      maxTemp: '--'
    },
    device: SceneJS ? JSON.parse(SceneJS.getDeviceInfo()) : {}
  },
  filters: {
    dateFormat: (date) => {
      return dateFormat(date, 'YYYY-mm-dd HH:MM')
    },
    androidVersion(sdkInt) {
      if (sdkInt) {
        const versions = {
          31: "Android 12",
          30: "Android 11",
          29: "Android 10",
          28: "Android 9",
          27: "Android 8.1",
          26: "Android 8.0",
          25: "Android 7.0",
          24: "Android 7.0",
          23: "Android 6.0",
          22: "Android 5.1",
          21: "Android 5.0"
        }
        return versions[sdkInt] || `SDK(${sdkInt})`
      } else {
        return '--'
      }
    }
  },
  mounted() {
    if (!this.$refs.myChart) {
      return
    }
    var ctx = this.$refs.myChart.getContext('2d');
    stackedLine = new Chart(ctx, {
      type: "line",
      data: {},
      options: {
        // events: ['click'],
        tooltips: {
          enabled: true
        },
        scales: {
          yAxes: [{
            position: 'left',
            offset: true,
            id: 'yB',
            ticks: {
              max: 50,
              min: 30,
              stepSize: 5
            }
          }, {
            position: 'right',
            offset: true,
            id: 'yA',
            ticks: {
              // max: 5,
              min: 0,
              // stepSize: 5
            }
          }],
          xAxes: [{
            ticks: {
              stepSize: 30
            }
          }]
        },
        legend: {
          display: true
        },
        elements: {
          line: {
            tension: 0 // 禁用贝塞尔曲线
          }
        },
        animation: {
          duration: 0 // 一般动画时间
        },
        hover: {
          animationDuration: 0 // 悬停项目时动画的持续时间
        },
        responsiveAnimationDuration: 0 // 调整大小后的动画持续时间
      }
    });
  },
  methods: {
    toggleFpsToolbar () {
      SceneJS.toggleFpsToolbar(!this.toolbarOn)
      setTimeout(() => {
        this.toolbarOn = JSON.parse(SceneJS.getFpsToolbarState())
      }, 100)
    },
    deleteSession(session) {
      SceneJS.deleteSession(session.sessionId);
      this.sessions.splice(this.sessions.indexOf(session), 1)
      if (this.sessions.length > 0) {
        this.onSessionClick(this.sessions[this.sessions.length - 1])
      }
    },
    onSessionClick(session) {
      const detail = {
        ...(JSON.parse(SceneJS.getSessionData(session.sessionId))),
        ...session
      };
      this.detail = detail;
      const fpsData = detail.fps || [];
      const temperatureData = detail.temperature || [];

      // 流畅度 (帧率 ≥40 的比例)
      if (fpsData.length > 0) {
        detail.fluency = '' + ((fpsData.filter(it => it >= 45).length) / fpsData.length * 100).toFixed(1)
      } else {
        detail.fluency = '--'
      }
      let maxTemp = 0
      temperatureData.forEach(it => {
        if (it > maxTemp) {
          maxTemp = it
        }
      });
      if (maxTemp > 0) {
        detail.highTempRatio = '' + ((temperatureData.filter(it => it >= 46).length) / temperatureData.length * 100).toFixed(1)
      } else {
        detail.highTempRatio = '--'
      }
      detail.maxTemp = maxTemp || '--'

      stackedLine.data = {
        vue: false,
        labels: fpsData.map((it, i) => {
          const minutes = parseInt(i / 60)
          const seconds = i % 60
          return '' + (minutes < 10 ? '0' : '') + minutes + ':' + (seconds < 10 ? '0' : '') + seconds
        }),
        datasets: [{
          label: '温度变化',
          steppedLine: false,
          yAxisID: 'yB',
          data: temperatureData,
          fill: false,
          borderColor: '#FF7E00',
          backgroundColor: '#FF7E00',
          borderWidth: 1,
          pointRadius: 0
        }, {
          label: '帧率变化',
          steppedLine: true,
          yAxisID: 'yA',
          data: fpsData,
          fill: true,
          backgroundColor: '#E3D5F1',
          borderColor: '#BC8DE4',
          borderWidth: 0.1,
          pointRadius: 0
        }]
      };
      stackedLine.update();
    }
  }
});
