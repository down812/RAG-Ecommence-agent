package com.jschaofan.ragagent.data.remote.sse

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatStreamDecoderTest {
    private val decoder = ChatStreamDecoder(
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        },
    )

    @Test
    fun `decodes content event without changing chunk text`() {
        val event = decoder.decode(
            SseFrame(
                event = "content",
                data = """{"type":"content","data":"12+2"}""",
            ),
        )

        assertTrue(event is ChatStreamEvent.Content)
        assertEquals("12+2", (event as ChatStreamEvent.Content).text)
    }

    @Test
    fun `decodes recommendation result from backend contract`() {
        val event = decoder.decode(
            SseFrame(
                event = "result",
                data = RESULT_EVENT,
            ),
        )

        assertTrue(event is ChatStreamEvent.Result)
        val result = (event as ChatStreamEvent.Result).value
        assertEquals("2", result.sessionId)
        assertEquals("001", result.messageId)
        assertEquals("recommendation", result.responseType)
        assertEquals("为你推荐以下手机", result.answer)
        assertEquals("智能手机", result.queryAnalysis?.detectedCategory)
        val recommendation = result.recommendations.orEmpty().first()
        assertEquals(1, result.recommendations.orEmpty().size)
        assertEquals(45L, recommendation.productId)
        assertEquals(3299.0, recommendation.price ?: 0.0, 0.0)
        assertEquals(
            "https://example.com/oppo.jpg",
            recommendation.mainImageUrl,
        )
    }

    @Test
    fun `decodes structured search result`() {
        val event = decoder.decode(
            SseFrame(
                event = "result",
                data = SEARCH_RESULT_EVENT,
            ),
        )

        assertTrue(event is ChatStreamEvent.Result)
        val result = (event as ChatStreamEvent.Result).value
        assertEquals("search_result", result.responseType)
        assertEquals("蓝牙耳机", result.searchCriteria?.keyword)
        assertEquals("200元以下", result.searchCriteria?.priceRange)
        assertEquals(1, result.totalCount)
        assertEquals(101L, result.products.orEmpty().first().productId)
        assertEquals("有货", result.products.orEmpty().first().stockStatus)
    }

    @Test
    fun `decodes image analysis and similar products`() {
        val event = decoder.decode(
            SseFrame(
                event = "result",
                data = IMAGE_SEARCH_EVENT,
            ),
        )

        assertTrue(event is ChatStreamEvent.Result)
        val result = (event as ChatStreamEvent.Result).value
        assertEquals("image_search", result.responseType)
        assertEquals("精华", result.imageAnalysis?.detectedCategory)
        assertEquals("雅诗兰黛", result.imageAnalysis?.detectedBrand)
        assertEquals("棕色瓶身", result.imageAnalysis?.visualFeatures?.first())
        val imageProduct = result.imageSearchProducts.orEmpty().first()
        assertEquals(201L, imageProduct.productId)
        assertEquals(0.96, imageProduct.similarity ?: 0.0, 0.0)
    }

    @Test
    fun `decodes typed rag source details`() {
        val event = decoder.decode(
            SseFrame(
                event = "result",
                data = SOURCE_RESULT_EVENT,
            ),
        )

        val result = (event as ChatStreamEvent.Result).value
        val source = result.sources.first()
        assertEquals("选购指南", source.title)
        assertEquals("商品介绍", source.ragSource?.productInfo)
        assertEquals("敏感肌可以使用吗？", source.ragSource?.official_faq?.first()?.question)
        assertEquals("用户A", source.ragSource?.user_reviews?.first()?.nickname)
    }

    @Test
    fun `decodes done and error events`() {
        val done = decoder.decode(
            SseFrame(
                event = "done",
                data = """{"type":"done","data":null}""",
            ),
        )
        val error = decoder.decode(
            SseFrame(
                event = "error",
                data = """
                    {
                      "type":"error",
                      "data":{
                        "code":"MODEL_TIMEOUT",
                        "message":"模型响应超时",
                        "retryable":true
                      }
                    }
                """.trimIndent(),
            ),
        )

        assertSame(ChatStreamEvent.Done, done)
        assertTrue(error is ChatStreamEvent.Error)
        val value = (error as ChatStreamEvent.Error).value
        assertEquals("MODEL_TIMEOUT", value.code)
        assertEquals("模型响应超时", value.message)
        assertTrue(value.retryable)
    }

    @Test
    fun `preserves unsupported event instead of crashing`() {
        val event = decoder.decode(
            SseFrame(
                event = "heartbeat",
                data = """{"type":"heartbeat","data":{"time":1}}""",
            ),
        )

        assertTrue(event is ChatStreamEvent.Unknown)
        assertEquals("heartbeat", (event as ChatStreamEvent.Unknown).type)
        assertFalse(event.rawData.isBlank())
    }

    private companion object {
        val RESULT_EVENT = """
            {
              "type":"result",
              "data":{
                "sessionId":"2",
                "messageId":"001",
                "responseType":"recommendation",
                "answer":"为你推荐以下手机",
                "sourcesStr":["来源"],
                "sources":[],
                "timestamp":null,
                "queryAnalysis":{
                  "detectedCategory":"智能手机",
                  "budget":"未指定",
                  "specialRequirements":[]
                },
                "recommendations":[
                  {
                    "productId":45,
                    "productName":"OPPO Reno 16 Pro",
                    "price":3299.0,
                    "brand":"OPPO",
                    "category":"数码电子 > 智能手机",
                    "mainImageUrl":"https://example.com/oppo.jpg",
                    "keyFeatures":["轻薄机身"],
                    "reason":"适合日常通勤",
                    "applicableScenario":"日常使用",
                    "rating":"4.7星",
                    "salesCount":0
                  }
                ],
                "comparedProducts":null,
                "comparisonMatrix":null,
                "comparisonRecommendations":null,
                "searchCriteria":null,
                "totalCount":null,
                "products":null,
                "futureField":"ignored"
              }
            }
        """.trimIndent()

        val SEARCH_RESULT_EVENT = """
            {
              "type":"result",
              "data":{
                "sessionId":"session-search",
                "messageId":"message-search",
                "responseType":"search_result",
                "answer":"找到1款商品",
                "sources":[],
                "timestamp":1780000000000,
                "searchCriteria":{
                  "keyword":"蓝牙耳机",
                  "brand":null,
                  "priceRange":"200元以下",
                  "category":"数码电子"
                },
                "totalCount":1,
                "products":[
                  {
                    "productId":101,
                    "productCode":"p_101",
                    "productName":"入门蓝牙耳机",
                    "brand":null,
                    "category":"蓝牙耳机",
                    "price":199.0,
                    "mainImageUrl":"https://example.com/101.jpg",
                    "status":"上架",
                    "salesCount":100,
                    "stockStatus":"有货",
                    "highlight":"价格匹配"
                  }
                ]
              }
            }
        """.trimIndent()

        val IMAGE_SEARCH_EVENT = """
            {
              "type":"result",
              "data":{
                "sessionId":"session-image",
                "messageId":"message-image",
                "responseType":"image_search",
                "answer":"识别到精华产品",
                "sources":[],
                "timestamp":1780000000000,
                "imageAnalysis":{
                  "detectedCategory":"精华",
                  "detectedBrand":"雅诗兰黛",
                  "visualFeatures":["棕色瓶身","金色瓶盖"],
                  "colorDescription":"琥珀色瓶身",
                  "shapeDescription":"圆柱形瓶身",
                  "textOnProduct":"Advanced Night Repair"
                },
                "imageSearchProducts":[
                  {
                    "productId":201,
                    "productCode":"beauty_201",
                    "productName":"小棕瓶精华",
                    "brand":"雅诗兰黛",
                    "category":"精华",
                    "price":899.0,
                    "mainImageUrl":"https://example.com/201.jpg",
                    "salesCount":500,
                    "similarity":0.96,
                    "matchReason":"品牌和外观高度吻合"
                  }
                ]
              }
            }
        """.trimIndent()

        val SOURCE_RESULT_EVENT = """
            {
              "type":"result",
              "data":{
                "sessionId":"session-source",
                "messageId":"message-source",
                "responseType":"recommendation",
                "answer":"参考知识库生成",
                "sources":[
                  {
                    "title":"选购指南",
                    "sourceType":"rag",
                    "content":null,
                    "ragSource":{
                      "productInfo":"商品介绍",
                      "marketing_description":"营销描述",
                      "official_faq":[
                        {
                          "question":"敏感肌可以使用吗？",
                          "answer":"建议先局部测试"
                        }
                      ],
                      "user_reviews":[
                        {
                          "nickname":"用户A",
                          "rating":"4.8",
                          "content":"使用感不错"
                        }
                      ]
                    }
                  }
                ],
                "timestamp":1780000000000,
                "queryAnalysis":null,
                "recommendations":[]
              }
            }
        """.trimIndent()
    }
}
