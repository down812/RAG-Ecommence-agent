package com.jschaofan.ragagent.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jschaofan.ragagent.data.remote.dto.ProductDetailDto
import com.jschaofan.ragagent.data.remote.dto.ProductSkuAttributeDto
import com.jschaofan.ragagent.data.remote.dto.ProductSkuDto

@Composable
internal fun AdminProductScreen(
    state: AdminUiState,
    viewModel: AdminViewModel,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<ProductDetailDto?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ProductDetailDto?>(null) }
    var imageProductId by remember { mutableStateOf<Long?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val productId = imageProductId
        if (uri != null && productId != null) viewModel.replaceProductImage(productId, uri)
        imageProductId = null
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { creating = true }) { Text("新建商品") }
                TextButton(onClick = viewModel::loadProducts) { Text("刷新") }
            }
        }
        items(state.products, key = ProductDetailDto::id) { product ->
            Card {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(product.title, fontWeight = FontWeight.Bold)
                    Text(
                        "${product.productCode} · ${product.brand.orEmpty()} · ¥${product.basePrice ?: 0.0}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("SKU：${product.skuList.size} · ${if (product.status == 1) "已上架" else "已下架"}")
                    Row {
                        TextButton(onClick = { editing = product }) { Text("编辑") }
                        TextButton(onClick = { viewModel.toggleProduct(product) }) {
                            Text(if (product.status == 1) "下架" else "上架")
                        }
                        TextButton(onClick = {
                            imageProductId = product.id
                            imagePicker.launch("image/*")
                        }) { Text("更换图片") }
                        TextButton(onClick = { deleting = product }) { Text("删除") }
                    }
                }
            }
        }
    }

    if (creating) {
        ProductEditorDialog(
            product = ProductDetailDto(status = 1),
            title = "新建商品",
            onDismiss = { creating = false },
            onSave = { product, imageUri ->
                viewModel.saveProduct(product, imageUri)
                creating = false
            },
        )
    }
    editing?.let { product ->
        ProductEditorDialog(
            product = product,
            title = "编辑商品",
            onDismiss = { editing = null },
            onSave = { updated, imageUri ->
                viewModel.saveProduct(updated, imageUri)
                editing = null
            },
        )
    }
    deleting?.let { product ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除商品") },
            text = { Text("确定删除“${product.title}”及其 SKU 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProduct(product.id)
                    deleting = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun ProductEditorDialog(
    product: ProductDetailDto,
    title: String,
    onDismiss: () -> Unit,
    onSave: (ProductDetailDto, android.net.Uri?) -> Unit,
) {
    var code by remember(product) { mutableStateOf(product.productCode) }
    var name by remember(product) { mutableStateOf(product.title) }
    var brand by remember(product) { mutableStateOf(product.brand.orEmpty()) }
    var category by remember(product) { mutableStateOf(product.category.orEmpty()) }
    var subCategory by remember(product) { mutableStateOf(product.subCategory.orEmpty()) }
    var price by remember(product) { mutableStateOf(product.basePrice?.toString().orEmpty()) }
    var imageUrl by remember(product) { mutableStateOf(product.mainImageUrl.orEmpty()) }
    var skus by remember(product) { mutableStateOf(product.skuList) }
    var selectedImage by remember(product) { mutableStateOf<android.net.Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedImage = it
    }
    var editingSku by remember { mutableStateOf<Pair<Int, ProductSkuDto>?>(null) }
    var addingSku by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("商品编码") }) }
                item { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("商品名称") }) }
                item { OutlinedTextField(brand, { brand = it }, Modifier.fillMaxWidth(), label = { Text("品牌") }) }
                item { OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("分类") }) }
                item { OutlinedTextField(subCategory, { subCategory = it }, Modifier.fillMaxWidth(), label = { Text("二级分类") }) }
                item { OutlinedTextField(price, { price = it }, Modifier.fillMaxWidth(), label = { Text("基础价格") }) }
                item { OutlinedTextField(imageUrl, { imageUrl = it }, Modifier.fillMaxWidth(), label = { Text("主图 URL") }) }
                item {
                    TextButton(onClick = { imagePicker.launch("image/*") }) {
                        Text(if (selectedImage == null) "选择本地主图" else "已选择主图，点击更换")
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SKU", fontWeight = FontWeight.Bold)
                        TextButton(onClick = { addingSku = true }) { Text("添加 SKU") }
                    }
                }
                items(skus.indices.toList(), key = { index -> "${skus[index].skuCode}-$index" }) { index ->
                    val sku = skus[index]
                    Card {
                        Column(Modifier.fillMaxWidth().padding(10.dp)) {
                            Text("${sku.skuCode} · ¥${sku.price ?: 0.0}")
                            Text(sku.attributeList.joinToString { "${it.attrName}=${it.attrValue}" })
                            Row {
                                TextButton(onClick = { editingSku = index to sku }) { Text("编辑") }
                                TextButton(onClick = { skus = skus.toMutableList().also { it.removeAt(index) } }) {
                                    Text("删除")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = code.isNotBlank() && name.isNotBlank() && price.toDoubleOrNull() != null,
                onClick = {
                    onSave(
                        product.copy(
                            productCode = code.trim(),
                            title = name.trim(),
                            brand = brand.trim().takeIf(String::isNotEmpty),
                            category = category.trim().takeIf(String::isNotEmpty),
                            subCategory = subCategory.trim().takeIf(String::isNotEmpty),
                            basePrice = price.toDouble(),
                            mainImageUrl = imageUrl.trim().takeIf(String::isNotEmpty),
                            skuList = skus,
                        ),
                        selectedImage,
                    )
                },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )

    if (addingSku) {
        SkuEditorDialog(ProductSkuDto(status = 1), onDismiss = { addingSku = false }) {
            skus = skus + it
            addingSku = false
        }
    }
    editingSku?.let { (index, sku) ->
        SkuEditorDialog(sku, onDismiss = { editingSku = null }) {
            skus = skus.toMutableList().also { list -> list[index] = it }
            editingSku = null
        }
    }
}

@Composable
private fun SkuEditorDialog(
    sku: ProductSkuDto,
    onDismiss: () -> Unit,
    onSave: (ProductSkuDto) -> Unit,
) {
    var code by remember(sku) { mutableStateOf(sku.skuCode) }
    var price by remember(sku) { mutableStateOf(sku.price?.toString().orEmpty()) }
    var attributes by remember(sku) {
        mutableStateOf(sku.attributeList.joinToString(", ") { "${it.attrName}=${it.attrValue}" })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑 SKU") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("SKU 编码") })
                OutlinedTextField(price, { price = it }, Modifier.fillMaxWidth(), label = { Text("价格") })
                OutlinedTextField(
                    attributes,
                    { attributes = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("属性（如 颜色=黑色, 尺码=M）") },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = code.isNotBlank() && price.toDoubleOrNull() != null,
                onClick = {
                    val attrs = attributes.split(",").mapNotNull { part ->
                        val pair = part.split("=", limit = 2).map(String::trim)
                        if (pair.size == 2 && pair.all(String::isNotBlank)) {
                            ProductSkuAttributeDto(attrName = pair[0], attrValue = pair[1])
                        } else null
                    }
                    onSave(sku.copy(skuCode = code.trim(), price = price.toDouble(), attributeList = attrs))
                },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
