package com.omarea.library.basic

import android.widget.CompoundButton

class RadioGroupSimulator {
    val radios = ArrayList<CompoundButton>()
    constructor(vararg items: CompoundButton) {
        items.iterator().forEach {
            radios.add(it)

            it.setOnClickListener {
                val isChecked = (it as CompoundButton).isChecked
                if(isChecked) {
                    this.autoUnChecked(it)
                } else {
                    this.autoCheck(it)
                }
            }
            it.setOnCheckedChangeListener { compoundButton, b ->
                if (b) {
                    autoUnChecked(compoundButton)
                }
            }
        }

        // 如果发现有多个选中，自动取消多余的选中（只保留最后一个）
        val checkedItems = radios.filter { it.isChecked }
        if (checkedItems.size > 0) {
            val last = checkedItems.last()
            checkedItems.forEach {
                if (it != last) {
                    it.isChecked = false
                }
            }
        }
    }

    private fun autoCheck(current: CompoundButton) {
        if (radios.filter { it.isChecked && it != current }.isEmpty()) {
            current.isChecked = true
        }
    }

    private fun autoUnChecked (current: CompoundButton) {
        radios.forEach {
            if (it != current) {
                it.isChecked = false
            }
        }
    }

    val checked: CompoundButton?
        get () {
            return radios.find { it.isChecked }
        }

    fun setCheckedByTag(value: Any) {
        val radio = radios.find { it.tag == value }
        radio?.isChecked = true
    }
}