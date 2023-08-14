package com.android.serialport2.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * @param modifier 应用于布局的修饰符
 * @param dataArray 数据数组
 * @param position 选择的item
 * @param expanded 是否展开
 * @param arrowColor 下拉箭头颜色
 * @param arrowSize 下拉箭头大小
 * @param maxShowHeight 下拉列表最大高度
 * @param enabled 是否启用
 * @param selectChange 选择item状态改变回调
 * @param expandedChange 列表 展开/收起 状态改变会标
 * @param itemContent 描述item的Compose组件内容。lambda参数：data为数据（dataArray[index])，modifier里写好了用于监听item的点击选择回调
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> Spinner(
    modifier: Modifier = Modifier,
    dataArray: Array<T>?,
    position: Int = 0,
    expanded: Boolean = false,
    arrowColor: Color = Color.LightGray,
    arrowSize: Dp = 30.dp,
    maxShowHeight: Dp = 100.dp,
    enabled: Boolean = true,
    selectChange: (Int, T) -> Unit = { _, _ -> },
    expandedChange: (Boolean) -> Unit = {},
    itemContent: @Composable (data: T, modifier: Modifier) -> Unit,
) {
    //下拉箭头旋转角度
    val degrees: Float by animateFloatAsState(targetValue = if (expanded) 0f else 90f, label = "")

    Column(modifier = modifier) {
        Row(modifier = Modifier.clickable(enabled = enabled) { expandedChange.invoke(!expanded) }) {
            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                //dataArray为空时使用Spacer占位
                if (dataArray == null) {
                    Spacer(modifier = Modifier.size(width = 100.dp, height = 38.dp))
                } else {
                    //调用itemContent显示item
                    itemContent.invoke(dataArray[position], Modifier)
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = if (expanded) "下拉" else "收起",
                tint = arrowColor,
                modifier = Modifier
                    .size(arrowSize)
                    .align(Alignment.CenterVertically)
                    .rotate(degrees)
            )
        }

        //dataArray不为空时才显示下拉列表
        if (dataArray != null) {
            //显示/隐藏动画
            AnimatedVisibility(expanded) {
                LazyColumn(modifier = Modifier.heightIn(max = maxShowHeight),
                    content = {
                        items(dataArray.size) {
                            itemContent.invoke(
                                dataArray[it],
                                Modifier.clickable(enabled = enabled) {
                                    selectChange.invoke(
                                        it,
                                        dataArray[it]
                                    )

                                }
                            )
                        }
                    }
                )
            }
        }

        //禁用时收起下拉列表
        if (!enabled && expanded) expandedChange.invoke(false)
    }
}

@Composable
fun <T> Spinner2(
    modifier: Modifier = Modifier,
    dropDownModifier: Modifier = Modifier,
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    selectedItemFactory: @Composable (Modifier, T) -> Unit,
    dropdownItemFactory: @Composable (T, Int) -> Unit,
) {
    var expanded: Boolean by remember { mutableStateOf(false) }
    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        selectedItemFactory(Modifier.clickable { expanded = true }, selectedItem)
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = dropDownModifier
        ) {
            items.forEachIndexed { index, element ->
                DropdownMenuItem(text = {
                    dropdownItemFactory(element, index)
                }, onClick = {
                    onItemSelected(items[index])
                    expanded = false
                })
            }
        }
    }
}


@Composable
fun MySpinner(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    Spinner2(
        modifier = Modifier
            .wrapContentSize()
            .border(
                width = 0.5.dp,
                color = Color.Black,
                shape = RoundedCornerShape(1.dp)
            ),
        dropDownModifier = Modifier.wrapContentSize(),
        items = items,
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        selectedItemFactory = { modifier, item ->
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = modifier
                    .padding(8.dp)
                    .wrapContentSize()
            ) {
                Text(item, modifier = Modifier.width(200.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "drop down arrow"
                )
            }
        },
        dropdownItemFactory = { item, _ ->
            Text(text = item)
        }
    )
}