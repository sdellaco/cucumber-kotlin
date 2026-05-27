package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.lang.LighterAST
import com.intellij.lang.LighterASTNode
import com.intellij.psi.impl.source.tree.RecursiveLighterASTNodeWalkingVisitor
import com.intellij.util.indexing.*
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.KotlinFileType.INSTANCE
import org.jetbrains.plugins.cucumber.CucumberStepIndex

val ANNOTATION_INDEX_ID = ID.create<Boolean, MutableList<Int>>("kotlin.cucumber.annotation.step")
private val PACKAGES_TO_SCAN = arrayOf("io.cucumber.java.", "cucumber.api.java.")

class CucumberKotlinAnnotationStepIndex : CucumberStepIndex() {

    override fun getName(): ID<Boolean, MutableList<Int>> = ANNOTATION_INDEX_ID

    override fun getVersion() = 1

    override fun hasSnapshotMapping(): Boolean {
        return true
    }

    override fun getPackagesToScan(): Array<String> = PACKAGES_TO_SCAN

    override fun getInputFilter(): FileBasedIndex.InputFilter =
        DefaultFileTypeSpecificInputFilter(INSTANCE)

    override fun getIndexer(): DataIndexer<Boolean, MutableList<Int>, FileContent> {
        // Override to support steps defined in subclasses
        return DataIndexer { inputData ->
            val text = inputData.contentAsText
            val lighterAst = (inputData as PsiDependentFileContent).lighterAST
            mapOf(true to getStepDefinitionOffsets(lighterAst, text))
        }
    }

    override fun getStepDefinitionOffsets(lighterAst: LighterAST, text: CharSequence): MutableList<Int> {
        val results = mutableListOf<Int>()

        val visitor = object : RecursiveLighterASTNodeWalkingVisitor(lighterAst) {
            override fun visitNode(element: LighterASTNode) {
                if (element.tokenType == KtNodeTypes.ANNOTATION_ENTRY) {
                    val children = lighterAst.getChildren(element)

                    // ANNOTATION_ENTRY children: AT, annotation name (REFERENCE_EXPRESSION or CONSTRUCTOR_CALLEE), optionally VALUE_ARGUMENT_LIST
                    val annotationName = children.firstOrNull {
                        it.tokenType == KtNodeTypes.CONSTRUCTOR_CALLEE ||
                                it.tokenType == KtNodeTypes.REFERENCE_EXPRESSION
                    }

                    if (annotationName != null && isStepDefinitionCall(annotationName, text)) {
                        val argList = children.firstOrNull { it.tokenType == KtNodeTypes.VALUE_ARGUMENT_LIST }
                        argList?.let {
                            lighterAst.getChildren(it).find { child -> child.tokenType == KtNodeTypes.VALUE_ARGUMENT }
                                ?.let { arg ->
                                    val regex = text.subSequence(arg.startOffset, arg.endOffset)
                                    if (regex.isNotEmpty() && regex != "\"\"") {
                                        results.add(element.startOffset)
                                    }
                                }
                        }
                    }
                }
                super.visitNode(element)
            }
        }
        visitor.visitNode(lighterAst.root)

        return results
    }

}
