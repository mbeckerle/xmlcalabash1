<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>
      <p:import href="atomic-imported.xpl"/>

      <p:choose>
        <p:when test="true()">
          <p:identity>
            <p:input port="source">
              <p:inline>
                <success/>
              </p:inline>
            </p:input>
          </p:identity>
        </p:when>
        <p:otherwise>
          <!-- the execution will never get here -->
          <test:imported xmlns:test="http://acme.com/test"/>
        </p:otherwise>
      </p:choose>

    </p:declare-step>