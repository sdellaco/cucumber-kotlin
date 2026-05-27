package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import groovy.json.StringEscapeUtils
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinAnnotationStepDefinition
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinAnnotationStepDefinitionCreator
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinParameterTypeManager
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.plugins.cucumber.BDDFrameworkType
import org.jetbrains.plugins.cucumber.StepDefinitionCreator
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

class CucumberKotlinAnnotationExtension : AbstractCucumberKotlinExtension() {

    override fun getStepFileType() = BDDFrameworkType(KotlinFileType.INSTANCE)

    override fun getStepDefinitionCreator(): StepDefinitionCreator = KotlinAnnotationStepDefinitionCreator()

    override fun loadStepsFor(featureFile: PsiFile?, module: Module): MutableList<AbstractStepDefinition> {
        val fileBasedIndex = FileBasedIndex.getInstance()
        val project = module.project

        val searchScope = module.getModuleWithDependenciesAndLibrariesScope(true)
            .uniteWith(ProjectScope.getLibrariesScope(project))
        val kotlinFiles = GlobalSearchScope.getScopeRestrictedByFileTypes(searchScope, KotlinFileType.INSTANCE)

        val elements = mutableListOf<KtAnnotationEntry>()
        fileBasedIndex.processValues(
            INDEX_ID,
            true,
            null,
            { file, offsets ->
                ProgressManager.checkCanceled()
                PsiManager.getInstance(project).findFile(file)?.let { psiFile ->
                    offsets.forEach { offset ->
                        val element = psiFile.findElementAt(offset + 1)
                        PsiTreeUtil.getParentOfType(element, KtAnnotationEntry::class.java)?.let { stepElement ->
                            elements.add(stepElement)
                        }
                    }
                }
                true
            },
            kotlinFiles
        )

        findParameterTypes(module, kotlinFiles)

        return elements.mapNotNull { stepElement ->
            if (CucumberKotlinUtil.isStepDefinition(stepElement)) {
                KotlinAnnotationStepDefinition(stepElement)
            } else null
        }.toMutableList()
    }

    private fun findParameterTypes(module: Module, kotlinFiles: GlobalSearchScope) {
        val occurrencesProcessor: (PsiElement, Int) -> Boolean = { element, _ ->
            element.parent?.let { parent ->
                if (parent is KtAnnotationEntry) {
                    handleParameterType(parent)
                }
            }
            true
        }

        ProgressManager.getInstance()
            .run(
                object : Task.Backgroundable(module.project, "Process elements with word", true) {
                    override fun run(indicator: ProgressIndicator) {
                        indicator.isIndeterminate = true
                        indicator.text = "Process elements with word..."
                        PsiSearchHelper.getInstance(module.project).processElementsWithWord(
                            occurrencesProcessor,
                            kotlinFiles,
                            "ParameterType",
                            UsageSearchContext.IN_CODE,
                            true
                        )
                    }
                })
    }

    private fun handleParameterType(element: PsiElement) {
        runCatching {
            val pointer = SmartPointerManager.getInstance(element.project).createSmartPsiElementPointer(element)

            val callExpression = element as? KtAnnotationEntry
            callExpression?.let {
                // "value" is the default attribute - may be unnamed (positional)
                val regexExpr =
                    it.valueArgumentList?.arguments?.firstOrNull { arg ->
                        val argName = arg.getArgumentName()?.asName?.asString()
                        argName == "value" || argName == null
                    }?.getArgumentExpression()
                regexExpr ?: return@let println("No regex argument: ${element.text}")
                val regex = (regexExpr as KtStringTemplateExpression).entries.joinToString("") { x -> x.text }

                // "name" is optional - fall back to the annotated method's name
                val nameExpr =
                    it.valueArgumentList?.arguments?.firstOrNull { arg ->
                        arg.getArgumentName()?.asName?.asString() == "name"
                    }?.getArgumentExpression()
                val name = if (nameExpr != null) {
                    (nameExpr as KtStringTemplateExpression).entries[0].text
                } else {
                    // Fall back to the name of the function the annotation is on
                    PsiTreeUtil.getParentOfType(it, KtNamedFunction::class.java)?.name
                        ?: return@let println("No name for parameter type: ${element.text}")
                }

                val unescapedRegex = StringEscapeUtils.unescapeJava(regex)
                KotlinParameterTypeManager.addParameterType(name, unescapedRegex, pointer)
            } ?: run {
                println("Not a call expression: ${element.text}") // TODO: Remove debug output
            }
        }
    }
}
